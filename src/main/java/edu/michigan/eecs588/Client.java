package edu.michigan.eecs588;

import edu.michigan.eecs588.Messenger.MMessage;
import edu.michigan.eecs588.Messenger.MessageReceived;
import edu.michigan.eecs588.Messenger.Messenger;
import edu.michigan.eecs588.authentication.ActiveAuthThread;
import edu.michigan.eecs588.authentication.AuthenticationFailureException;
import edu.michigan.eecs588.authentication.GroupVerificationThread;
import edu.michigan.eecs588.authentication.PrivateChat;
import edu.michigan.eecs588.encryption.*;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.stringencoder.java7.Base64;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Entry point for the client for the XMPP.
 */
public class Client {

	private Map<String, String> configFile;
	private MultiUserChat muc;
	private AbstractXMPPConnection connection;
	private String roomName;
	private ChatManagerListener listener;
	private final Object LOCK;
	private ECMQVKeyPair longTermKeyPair;
	private RSAKeyPair keyPair;
	private final TreeMap<String, String> participants;
	private final TreeMap<String, PrivateChat> privateChats;
	private int verificationCount;
	private boolean verificationSucceeds;
	private AESCrypto rootKey;
	private boolean isInitiator;
	private Printer printer;
	private boolean setupWillStart;
	private MessageListener simpleMessageListener;

	private Messenger messenger;

	/* For testing purposes. */
	private Map<String, Verifier> publicKeys = new HashMap<>();

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
		
		longTermKeyPair = new ECMQVKeyPair();
		keyPair = new RSAKeyPair();
		participants = new TreeMap<>();
		privateChats = new TreeMap<>();
		LOCK = new Object();
		isInitiator = false;
		printer = new Printer(this);
		setupWillStart = false;
		
		this.simpleMessageListener = new MessageListener() {
			@Override
			public void processMessage(Message message) {
				System.out.println(message.getBody());
			}
		}; 
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

		longTermKeyPair = new ECMQVKeyPair();
		keyPair = new RSAKeyPair();
		participants = new TreeMap<>();
		privateChats = new TreeMap<>();
		LOCK = new Object();
		isInitiator = false;
		printer = new Printer(this);
		setupWillStart = false;
		
		this.simpleMessageListener = new MessageListener() {
			@Override
			public void processMessage(Message message) {
				printer.println(message.getBody());
			}
		}; 
	}

	public String getRoomName()
	{
		return roomName;
	}

	public Printer getPrinter()
	{
		return printer;
	}

	/**
	 * Setup the chat listener for private messaging (pairwise).
	 */
	private void setupChatListener() {
		listener = new ChatManagerListener()
		{
			@Override
			public void chatCreated(Chat chat, boolean createdLocally)
			{
				if (!createdLocally && setupWillStart)
				{
					try
					{
						PrivateChat privateChat = new PrivateChat(chat);
						privateChats.put(chat.getParticipant(), privateChat);
						(new Thread(new ActiveAuthThread(Client.this, privateChat, longTermKeyPair, keyPair))).start();
					}
					catch (NotConnectedException e)
					{
						throw new RuntimeException(e);
					}
				}
			}
		};
		ChatManager.getInstanceFor(Client.this.connection).addChatListener(listener);
	}

	public MultiUserChat getMultiUserChat() {
		return muc;
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
		setStatus(connection, true, "ONLINE");
		roomName = "EECS588";
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
		printer.println("Welcome to " + muc.getRoom());
		printer.println("Type your message and press Enter to send.");
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
		this.roomName = muc.getRoom();
    }

    /**
     * Adds an invitation listener to the connection
     *
     * @param xmppConnection the connection to add to
     */
    public void addInvitationListener(AbstractXMPPConnection xmppConnection) {
    	MultiUserChatManager.getInstanceFor(xmppConnection).addInvitationListener(new InvitationListener()
		{
			@Override
			public void invitationReceived(XMPPConnection connection, MultiUserChat muc,
										   String inviter, String reason, String password, Message message)
			{
				try
				{
					printer.println("Received an invitation to join: " + muc.getRoom());
					muc.join(configFile.get("username"));
					Client.this.muc = muc;
					Client.this.roomName = muc.getRoom();
					Client.this.muc.addMessageListener(Client.this.simpleMessageListener);
					printer.println("Joined: " + muc.getRoom());
					setupWillStart = true;
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
		isInitiator = true;
		setupWillStart = true;
    	String username = this.generateUsername(user);
    	printer.println("Inviting " + user);
    	muc.invite(username, "I love you");
    }

    private String generateUsername(String user) {
    	return user + "@" + configFile.get("serviceName");
    }

	private Messenger createMessenger(Map<String, Verifier> publicKeys, Signer sign) {
		printer.println("Hello! " + this.muc);
		return new Messenger(this.getMultiUserChat(), new MessageReceived() {
			@Override
			public void onMessageReceived(MMessage message) {
				String messageBody = message.getMessage();
				if (messageBody.equals("$setup"))
				{
					if (!isInitiator)
					{
						(new Thread(new Runnable()
						{
							@Override
							public void run()
							{
								try
								{
									authenticate();
								}
								catch (AuthenticationFailureException e)
								{
									printer.println("Authentication failed.");
								}
							}
						})).start();
					}
				}
				else
				{
					System.out.println(message.getUsername() + ": " + messageBody);
					printer.print();
				}
			}
		}, publicKeys, sign, rootKey.getSecret());
	}
    
    /**
     * Creates a private message chat.
     *
     * @param user the user to chat with
     * @return the chat
     * @throws NotConnectedException
     */
    public Chat createPrivateChat(String user) throws NotConnectedException {;
    	return muc.createPrivateChat(user, null);
    }

	/**
	 * Stop inviting people and start the set up phase
	 */
	public void setup() throws NotConnectedException
	{
		getMessenger().sendMessage("$setup");
		try
		{
			authenticate();
		}
		catch (AuthenticationFailureException e)
		{
			printer.println("Authentication failed");
		}
	}

	/**
	 * Do the authentication phase.
	 */
	public void authenticate() throws AuthenticationFailureException
	{
		List<String> occupants = new ArrayList<>(muc.getOccupants());
		Collections.sort(occupants);

		try
		{
			/* Step 1: exchange public keys with all other participants */
			printer.println("Exchanging public keys with the other participants...");
			exchangePublicKeys(occupants);
			printer.println("Public key exchange done.");

			/* Step 2: verify that all participants have the same view of the public keys */
			printer.println("Verifying public keys...");
			verifyPublicKeys();
			printer.println("Public key verification done.");

			/* Step 3: the first participant(in alphabetic order) generates the root key
			*          and sends it to all other participants */
			printer.println("Generating root key...");
			sendOrReceiveRootKey();
			printer.println("Root key generated and sent.");

			/* Step 4: verify that all participants have the same root key */
			printer.println("Verifying root key...");
			verifyRootKey();
			printer.println("Root key verification done.");

			for (Map.Entry<String, PrivateChat> privateChat : privateChats.entrySet())
			{
				privateChat.getValue().close();
			}

			printer.println("Authentication done.");
			this.muc.removeMessageListener(this.simpleMessageListener);
			for (Map.Entry<String, String> participant : participants.entrySet())
			{
				publicKeys.put(participant.getKey(), new Verifier(participant.getValue()));
			}
			this.messenger = this.createMessenger(publicKeys, new Signer(keyPair.getPrivateKey()));
			if (!isInitiator)
			{
				printer.print();
			}
		}
		catch (NoSuchAlgorithmException | InterruptedException | UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		catch (NotConnectedException e)
		{
			throw new RuntimeException(e);
		}
	}

	private void exchangePublicKeys(List<String> occupants)
	{
		String meInChatRoom = getUsername();
		int indexOfUser = occupants.indexOf(meInChatRoom) + 1;

		// Participants only establish private chat with participants after him in alphabetical order
		for (; indexOfUser < occupants.size(); ++indexOfUser)
		{
			try
			{
				String user = occupants.get(indexOfUser);
				PrivateChat chat = new PrivateChat(muc, user);
				privateChats.put(user, chat);
				(new Thread(new ActiveAuthThread(Client.this, chat, longTermKeyPair, keyPair))).start();
			}
			catch (NotConnectedException e)
			{
				throw new RuntimeException(e);
			}
		}

		synchronized (LOCK)
		{
			try
			{
				LOCK.wait();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		participants.put(getUsername(), keyPair.getPublicKeyAsString());
		ChatManager.getInstanceFor(connection).removeChatListener(listener);
	}

	private void verifyPublicKeys() throws NoSuchAlgorithmException, AuthenticationFailureException, InterruptedException
	{
		MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
		for (Map.Entry<String, String> participant : participants.entrySet())
		{
			String publicKey = participant.getValue();

			if (publicKey == null)
			{
				throw new AuthenticationFailureException();
			}
			else
			{
				try
				{
					sha256Digest.update(participant.getKey().getBytes("UTF-8"));
					sha256Digest.update(publicKey.getBytes("UTF-8"));
				}
				catch (UnsupportedEncodingException e)
				{
					e.printStackTrace();
				}
			}
		}

		verifyHash(Base64.encodeBytes(sha256Digest.digest()));
	}

	private void sendOrReceiveRootKey() throws NotConnectedException, InterruptedException
	{
		if (participants.firstKey().equals(getUsername()))
		{
			// I am the first user. Generate a root key and send to the others
			rootKey = new AESCrypto();
			for (Map.Entry<String, PrivateChat> privateChat : privateChats.entrySet())
			{
				privateChat.getValue().sendMessage(rootKey.getSecret());
			}
		}
		else
		{
			PrivateChat chatWithFirstParticipant = privateChats.firstEntry().getValue();
			rootKey = new AESCrypto(chatWithFirstParticipant.nextMessage());
		}
	}

	private String getUsername()
	{
		return roomName + "/" + configFile.get("username");
	}

	private void verifyRootKey() throws NoSuchAlgorithmException, UnsupportedEncodingException, InterruptedException, AuthenticationFailureException
	{
		MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
		String hash = Base64.encodeBytes(sha256Digest.digest(rootKey.getSecret().getBytes("UTF-8")));
		verifyHash(hash);
	}

	private void verifyHash(String hash) throws InterruptedException, AuthenticationFailureException
	{
		verificationCount = 0;
		verificationSucceeds = true;

		for (Map.Entry<String, PrivateChat> privateChat : privateChats.entrySet())
		{
			try
			{
				PrivateChat chat = privateChat.getValue();
				(new Thread(new GroupVerificationThread(Client.this, chat, hash))).start();
			}
			catch (NotConnectedException e)
			{
				throw new RuntimeException(e);
			}
		}
		synchronized (LOCK)
		{
			LOCK.wait();
		}

		if (!verificationSucceeds)
		{
			throw new AuthenticationFailureException();
		}
	}

	/**
	 * Called when the pairwise authentication is done.
	 * @param anotherUser The user which this client does pairwise authentication with
	 * @param publicKey The public key of the user. Null if authentication fails.
	 */
	public void authDone(String anotherUser, String publicKey)
	{
		synchronized (LOCK)
		{
			participants.put(anotherUser, publicKey);
			if (participants.size() == muc.getOccupantsCount() - 1)
			{
				LOCK.notify();
			}
		}
	}

	/**
	 * Called when a pairwise verification is done
	 * @param verificationSucceeds Whether or not the verification succeeds
	 */
	public void verify(boolean verificationSucceeds)
	{
		this.verificationSucceeds = this.verificationSucceeds && verificationSucceeds;
		synchronized (LOCK)
		{
			++verificationCount;
			if (verificationCount == muc.getOccupantsCount() - 1)
			{
				LOCK.notify();
			}
		}
	}
}
