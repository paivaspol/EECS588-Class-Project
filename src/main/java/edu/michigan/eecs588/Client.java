package edu.michigan.eecs588;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.michigan.eecs588.encryption.ECMQVKeyPair;
import edu.michigan.eecs588.encryption.RSAKeyPair;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;

/**
 * Entry point for the client for the XMPP.
 */
public class Client {
	
	private Map<String, String> configFile;
	private MultiUserChat muc;
	private AbstractXMPPConnection connection;
	private String roomName;
	private ECMQVKeyPair longTermKeyPair;
	private RSAKeyPair keyPair;
	private final TreeMap<String, String> participants;
    
	/**
	 * Constructs the client.
	 * @throws IOException 
	 * @throws XMPPException 
	 * @throws SmackException 
	 */
	public Client() throws IOException, SmackException, XMPPException {
		this.configFile = ConfigFileReader.getConfigValues();
		this.connection = createConnectionAndLogin(configFile);
		addInvitationListener(connection);
		setupChatListener();
		participants = new TreeMap<>();
		longTermKeyPair = new ECMQVKeyPair();
	}
	
	/**
	 * Constructs the client.
	 * 
	 * @param configFilename the config file name
	 * @throws IOException
	 * @throws SmackException
	 * @throws XMPPException
	 */
	public Client(String configFilename) throws IOException, SmackException, XMPPException {
		this.configFile = ConfigFileReader.getConfigValues(configFilename);
		this.connection = createConnectionAndLogin(configFile);
		addInvitationListener(connection);
		setupChatListener();
		participants = new TreeMap<>();
		longTermKeyPair = new ECMQVKeyPair();
	}
	
	/**
	 * Setup the chat listener for private messaging (pairwise).
	 */
	private void setupChatListener() {
		ChatManager.getInstanceFor(connection).addChatListener(new ChatManagerListener()
		{
			@Override
			public void chatCreated(Chat chat, boolean createdLocally)
			{
				if (!createdLocally)
				{
//					(new Thread(new PassiveAuthThread())).start();
				}
			}
		});
	}
	
	public MultiUserChat getMultiUserChat() {
		return muc;
	}
	
	public AbstractXMPPConnection getConnection() {
		return connection;
	}
    
    /**
     * Connects to the xmpp server.
     * 
     * @throws SmackException
     * @throws IOException
     * @throws XMPPException
     */
    public AbstractXMPPConnection createConnectionAndLogin(Map<String, String> configFile) throws SmackException, IOException, XMPPException {
		XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
				.setServiceName(configFile.get("serviceName"))
				.setUsernameAndPassword(configFile.get("username"), configFile.get("password"))
				.setHost(configFile.get("host"))
				.setPort(Integer.valueOf(configFile.get("port")))
				.setSecurityMode(SecurityMode.disabled)
				.build();
		String username = configFile.get("username");
		AbstractXMPPConnection connection = new XMPPTCPConnection(config);
		connection.connect();
		connection.login(configFile.get("username"), configFile.get("password"));
		setStatus(connection, true, "ONLINE");
		System.out.println("Connected to XMPP server with " + username);
    	return connection;
    }
    
    public static void setStatus(AbstractXMPPConnection connection, boolean available, String status) throws XMPPException, NotConnectedException {
        Presence.Type type = available ? Type.available : Type.unavailable;
        Presence presence = new Presence(type);
        presence.setStatus(status);
        connection.sendStanza(presence);
    }
    
    /**
     * 
     * @param roomName Name of the room to be created
     * @throws XMPPErrorException 
     * @throws SmackException 
     */
    public void createRoom(String roomName) throws XMPPErrorException, SmackException {
    	MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
    	String multiChatService = configFile.get("multiUserChatService");
		MultiUserChat muc = manager.getMultiUserChat(roomName + "@" + multiChatService);
		muc.create(configFile.get("username"));
		System.out.println("Welcome to " + muc.getRoom());
		System.out.println("Type your message and press Enter to send.");
		// Get the the room's configuration form
		Form form = muc.getConfigurationForm();
		// Create a new form to submit based on the original form
		Form submitForm = form.createAnswerForm();
		// Add default answers to the form to submit
		for (FormField field : form.getFields()) {
			if (!FormField.Type.hidden.equals(field.getType()) && field.getVariable() != null) {
				// Sets the default value as the answer
				submitForm.setDefaultAnswer(field.getVariable());
			}
		}
		// Sets the new owner of the room
		List<String> owners = new ArrayList<>();
		owners.add(this.generateUsername(configFile.get("username")));
		submitForm.setAnswer("muc#roomconfig_roomowners", owners);
		// Send the completed form (with default values) to the server to configure the room
		muc.sendConfigurationForm(submitForm);
		this.muc = muc;
		this.roomName = roomName;
    }
    
    /**
     * Adds an invitation listener to the connection
     * 
     * @param connection the connection to add to
     */
    public void addInvitationListener(AbstractXMPPConnection connection) {
    	MultiUserChatManager.getInstanceFor(connection).addInvitationListener(new InvitationListener()
		{
			@Override
			public void invitationReceived(XMPPConnection connection, MultiUserChat muc,
										   String inviter, String reason, String password, Message message)
			{
				try
				{
					System.out.println("Received an invitation to join: " + muc.getRoom());
					muc.join(configFile.get("username"));
					Client.this.muc = muc;
					System.out.println("Joined: " + muc.getRoom());
				}
				catch (NoResponseException | XMPPErrorException
						| NotConnectedException e)
				{
					throw new RuntimeException(e);
				}
			}
		});
    }
    
    /**
     * Invites a user.
     * 
     * @param user the user
     * @throws NotConnectedException if not connected
     */
    public void inviteParticipant(String user) throws NotConnectedException {
    	String username = this.generateUsername(user);
    	System.out.println("Inviting " + user);
    	muc.invite(username, "I love you");
    }
    
    private String generateUsername(String user) {
    	return user + "@" + configFile.get("serviceName");
    }
    
    /**
     * Creates a private message chat.
     * 
     * @param user the user to chat with
     * @return the chat
     * @throws NotConnectedException
     */
    public Chat createPrivateChat(String user) throws NotConnectedException {
    	String chatDestination = roomName + "@" + configFile.get("multiUserChatService") + "/" + user;
    	return muc.createPrivateChat(chatDestination, null);
    }

	/**
	 * Do the authentication phase.
	 */
	public void authenticate() throws AuthenticationFailtureException
	{
		keyPair = new RSAKeyPair();
		List<String> occupants = muc.getOccupants();
		int indexOfMe = occupants.indexOf(configFile.get("username"));
		for (; indexOfMe < occupants.size() - 1; ++indexOfMe)
		{
			try
			{
				(new Thread(new ActiveAuthThread(Client.this, occupants.get(indexOfMe), longTermKeyPair, keyPair))).start();
			}
			catch (NotConnectedException e)
			{
				throw new RuntimeException(e);
			}
		}

		synchronized (participants)
		{
			try
			{
				participants.wait();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}


		for (Map.Entry<String, String> keyValue : participants.entrySet())
		{
		}
	}

	/**
	 * Called when the pairwise authentication is done.
	 * @param anotherUser The user which this client does pairwise authentication with
	 * @param publicKey The public key of the user. Null if authentication fails.
	 */
	public void authDone(String anotherUser, String publicKey)
	{
		synchronized (participants)
		{
			participants.put(anotherUser, publicKey);
			if (participants.size() == muc.getOccupantsCount())
			{
				participants.notify();
			}
		}
	}
}
