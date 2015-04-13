package edu.michigan.eecs588;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Map;

import edu.michigan.eecs588.Messenger.MMessage;
import edu.michigan.eecs588.Messenger.MessageReceived;
import edu.michigan.eecs588.Messenger.Messenger;
import edu.michigan.eecs588.encryption.RSAKeyPair;
import edu.michigan.eecs588.encryption.Signer;
import edu.michigan.eecs588.encryption.Verifier;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
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
	private Messenger messenger;
	
	/* For testing purposes. */
	Map<String, Verifier> publicKeys = new HashMap<String, Verifier>();
	RSAKeyPair X = new RSAKeyPair();
	Signer sign = new Signer(X.getPrivateKey());
    
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
		
		/* For testing purposes. */
		Verifier veri = new Verifier(X.getPublicKey());
		publicKeys.put(configFile.get("username"), veri);
		publicKeys.put("d", veri);
	}
	
	public MultiUserChat getMultiUserChat() {
		return muc;
	}
	
	public AbstractXMPPConnection getConnection() {
		return connection;
	}

	public Map<String, String> getConfigFile() {
		return configFile;
	}
	
	public Messenger getMessenger() {
		return messenger;
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
		setStatus(connection, true,"ONLINE");
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
     * @param connection
     * @param roomName
     * @throws XMPPErrorException 
     * @throws SmackException 
     */
    public void createRoom(String roomname) throws XMPPErrorException, SmackException {
    	MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
    	String multiChatService = configFile.get("multiUserChatService");
		MultiUserChat muc = manager.getMultiUserChat(roomname + "@" + multiChatService);
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
		this.messenger = this.createMessenger(publicKeys, sign);
    }
    
    /**
     * Adds an invitation listener to the connection
     * 
     * @param connection the connection to add to
     */
    public void addInvitationListener(AbstractXMPPConnection connection) {
    	MultiUserChatManager.getInstanceFor(connection).addInvitationListener(new InvitationListener() {
			@Override
			public void invitationReceived(XMPPConnection connection, MultiUserChat muc,
					String inviter, String reason, String password, Message message) {
				try {
					System.out.println("Received an invitation to join: " + muc.getRoom().toString());
					muc.join(configFile.get("username"));
					Client.this.muc = muc;
					Client.this.messenger = Client.this.createMessenger(publicKeys, sign);
					System.out.println("Joined: " + muc.getRoom().toString());
				} catch (NoResponseException | XMPPErrorException
						| NotConnectedException e) {
					throw new RuntimeException(e);
				}
			}
		});
    }
    
    /**
     * Invites a user.
     * 
     * @param muc the multi-user chat object
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

	private Messenger createMessenger(Map<String, Verifier> publicKeys, Signer sign) {
		System.out.println("Hello! " + this.muc);
		return new Messenger(this.getMultiUserChat(), new MessageReceived() {
			@Override
			public void onMessageReceived(MMessage message) {
				System.out.println(message.getUsername() + ": " + message.getMessage());
			}
		}, publicKeys, sign, "2xil0x35oH8onjyLeudMlP+5h18r/HZ3drd3WXrqm9I=");
	}
}
