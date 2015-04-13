package edu.michigan.eecs588;

import edu.michigan.eecs588.encryption.AESCrypto;
import edu.michigan.eecs588.encryption.ECMQVKeyPair;
import edu.michigan.eecs588.encryption.RSAKeyPair;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
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
	private final Map<String, AESCrypto> cryptoes;
	private final TreeMap<String, Chat> privateChats;
	private int verificationCount;
	private boolean verificationSucceeds;
	private AESCrypto rootKey;

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

		longTermKeyPair = new ECMQVKeyPair();
		participants = new TreeMap<>();
		cryptoes = new HashMap<>();
		privateChats = new TreeMap<>();
		LOCK = new Object();
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
		participants = new TreeMap<>();
		cryptoes = new HashMap<>();
		privateChats = new TreeMap<>();
		LOCK = new Object();
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
				if (!createdLocally)
				{
					try
					{
						privateChats.put(chat.getParticipant(), chat);
						(new Thread(new PassiveAuthThread(Client.this, chat.getParticipant(), longTermKeyPair, keyPair))).start();
					}
					catch (NotConnectedException e)
					{
						throw new RuntimeException(e);
					}
				}
			}
		};
		ChatManager.getInstanceFor(connection).addChatListener(listener);
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
		List<String> occupants = muc.getOccupants();
		Collections.sort(occupants);

		try
		{

			/* Step 1: exchange public keys with all other participants */
			exchangePublicKeys(occupants);

			/* Step 2: verify that all participants have the same view of the public keys */
			verifyPublicKeys();

			/* Step 3: the first participant(in alphabetic order) generates the root key
			*          and sends it to all other participants */
			sendOrReceiveRootKey();

			/* Step 4: verify that all participants have the same root key */
			verifyRootKey();

			for (Map.Entry<String, Chat> privateChat : privateChats.entrySet())
			{
				privateChat.getValue().close();
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
		keyPair = new RSAKeyPair();
		int indexOfUser = occupants.indexOf(configFile.get("username"));

		// Participants only establish private chat with participants after him in alphabetical order
		for (; indexOfUser < occupants.size() - 1; ++indexOfUser)
		{
			try
			{
				String user = occupants.get(indexOfUser);
				Chat chat = createPrivateChat(user);
				privateChats.put(user, chat);
				(new Thread(new ActiveAuthThread(Client.this, chat, user, longTermKeyPair, keyPair))).start();
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
		ChatManager.getInstanceFor(connection).removeChatListener(listener);
	}

	private void verifyPublicKeys() throws NoSuchAlgorithmException, AuthenticationFailtureException, InterruptedException
	{
		MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
		for (Map.Entry<String, String> keyValue : participants.entrySet())
		{
			if (keyValue.getValue() == null)
			{
				throw new AuthenticationFailtureException();
			}
			else
			{
				try
				{
					sha256Digest.update(keyValue.getKey().getBytes("UTF-8"));
					sha256Digest.update(keyValue.getValue().getBytes("UTF-8"));
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
		if (participants.firstKey().equals(configFile.get("username")))
		{
			// I am the first user. Generate a root key and send to the others
			rootKey = new AESCrypto();
			for (Map.Entry<String, Chat> privateChat : privateChats.entrySet())
			{
				privateChat.getValue().sendMessage(rootKey.getSecret());
			}
		}
		else
		{
			Chat chatWithFirstParticipant = privateChats.firstEntry().getValue();
			final ChatMessageListener listener = new ChatMessageListener()
			{
				@Override
				public void processMessage(Chat chat, Message message)
				{
					rootKey = new AESCrypto(message.getBody());

					synchronized (LOCK)
					{
						LOCK.notify();
					}
				}
			};
			chatWithFirstParticipant.addMessageListener(listener);
			synchronized (LOCK)
			{
				LOCK.wait();
				chatWithFirstParticipant.removeMessageListener(listener);
			}
		}
	}

	private void verifyRootKey() throws NoSuchAlgorithmException, UnsupportedEncodingException, InterruptedException, AuthenticationFailtureException
	{
		MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
		String hash = Base64.encodeBytes(sha256Digest.digest(rootKey.getSecret().getBytes("UTF-8")));
		verifyHash(hash);
	}

	private void verifyHash(String hash) throws InterruptedException, AuthenticationFailtureException
	{
		verificationCount = 0;
		verificationSucceeds = true;

		for (Map.Entry<String, Chat> privateChat : privateChats.entrySet())
		{
			try
			{
				String user = privateChat.getKey();
				Chat chat = privateChat.getValue();
				(new Thread(new GroupVerificationThread(Client.this, chat, hash, cryptoes.get(user)))).start();
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
			throw new AuthenticationFailtureException();
		}
	}

	/**
	 * Called when a pairwise MQV key agreement is done
	 * @param anotherUser The user with which the agreement happens
	 * @param crypto The symmetric key derived from MQV
	 */
	public void MQVDone(String anotherUser, AESCrypto crypto)
	{
		synchronized (cryptoes)
		{
			cryptoes.put(anotherUser, crypto);
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
			if (participants.size() == muc.getOccupantsCount())
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
			if (verificationCount == muc.getOccupantsCount())
			{
				LOCK.notify();
			}
		}
	}
}
