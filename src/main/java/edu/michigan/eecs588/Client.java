package edu.michigan.eecs588;

import edu.michigan.eecs588.Messenger.MMessage;
import edu.michigan.eecs588.Messenger.MessageReceived;
import edu.michigan.eecs588.Messenger.Messenger;
import edu.michigan.eecs588.encryption.*;
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
	private boolean isInitiator;

	private Messenger messenger;
	
	/* For testing purposes. */
	private String privateKey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC4eYiG8Atjnu6ePk3vZn7yZR7U" +
								"Om9kY5hQtQYutLFYJoeZ5ivffhE+N/9xm0fp9xZ8FquRFaZGVcau3mjstyQL456oOgQ2BJ0h7lO3" +
								"JN0khMd/LtXw8rrn/Im5EV+qoQpNWNv9W8f6+yTm7BanSwosMhiWF3ie/UKjqt+jngYKYFnhwqGD" +
								"cDc3j+UB6RDerXDlQloTSoVSS3kNPZ3bhq377fEzHOuYntXT9/4H+OlnyfMmyQk95QT/m5h/B/Oh" +
								"vI4/w9MkXt49TmuPLS7f6ihnQYJ8o6S2BThlbedbqLXheJVFIP0L1978ct4A3NWMCH7ClpVNm62c" +
								"6FDJNZadajJxAgMBAAECggEASDYawWd5rddq5LrS2yGuE8iNltoA/LUXzI/wCZxlx3Hdptq41dWN" +
								"fmOBNMFqFyXHwW9GXZax7dpp2c3qGK9gBt9lHckIGPUZZUzbrFdb0Y3AYgK9cIIzs9fhOXaZkjtT" +
								"ww9DnhutXous2FAWVgpDwyUhBq/NYb8VtOeQf4W+K5T9QdU+jaOWGw/+LgmuNeOgMrx9Z9Uma+JN" +
								"7E++o17P4EIdDE+J4yuHAlonZGRshzXGZ8lPM42YDm8htoB6dGSQryvRBrhx7IV2LY6/91pKUiGU" +
								"0KiGfnMt7yhhyZ+6TXs/wLA30Z8xGZGrFkqxbGzUSYmoZPlijeYrIOw3P4K2eQKBgQDp5NldaGWy" +
								"0LPn6d+u6EBT2o5kaS30t9Q8O8IhTe4RuwHTr+0yRFEEtH93mcCqDoJk4K7rh+ae+KKQyfQyv1F6" +
								"ZUPZjIc0WQxyKh6r1mixk02EbR2Xu17h321iApLil7NUAmgwFBIeilImQ/T7DZOF04epyPz6Uf6M" +
								"9ZBvNgo+cwKBgQDJ6PffEeYi8N4DD52lvnIpc7buk4OBU0TJMi6E9tbySAwiekye+lHTnm5t0Sci" +
								"ZLt5ny1OQoeDYpFTz3rEP6bCyYK/RVJg2yShDQh4KLeiF6vVzZv5EkNiBA2QXVxv59yNrhh+ZSvt" +
								"0o6FAV0Mk7/eBuN46dk92PddsPb/49AOiwKBgQDLOCmcPQ7PtQH7aHsPT4BE20qI99IUJHzk8M8V" +
								"fM5y3VcTBHJFEJKMUf/GfPKnTwAAgi+5OVUpnsIwpyfjx9MVU1MGfFElLJHhx7LqftpsH55khyIF" +
								"Kamg+y3g0HerJT+MqKK501aC7o+966G5V+xrhIUFAjq7i+5trxcVaZCw/QKBgBig3CtPiZzVeJ8y" +
								"9m3TLyWzQasUP0Q1HfzUrCv31/wxoZlLBbGWowiHGL04d+eQFfYd3m7fWpxCF6v7cOQjR3oXDmW6" +
								"VPQPhwZGCrRtcwjIjmND8zSMb7+y8tybJr1XsOVvSPmR6avWtR+wLONt2keA25pSG/eZUYMSJO7N" +
								"oQFFAoGBAJ1zwzrRJp2uMbVM9V6cNe1EYGaMviKJzYadBbyA3a8JaKYBm2nctyBTGsm5yoZJd+6B" +
								"NIe6GoVmp9u9GpQvdcL4QnG2JJ86NyqojUTh1XC5Mc903dRxPkcyydssxyE0dxldkKeo2mpvVBMq" +
								"gDvVTQPycQJgFgwKiKocFzLApdY5" ;		
			
	private String publicKey = 	"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuHmIhvALY57unj5N72Z+8mUe1DpvZGOYU" +
							 	"LUGLrSxWCaHmeYr334RPjf/cZtH6fcWfBarkRWmRlXGrt5o7LckC+OeqDoENgSdIe5TtyTdJITHfy" +
							 	"7V8PK65/yJuRFfqqEKTVjb/VvH+vsk5uwWp0sKLDIYlhd4nv1Co6rfo54GCmBZ4cKhg3A3N4/lAek" +
							 	"Q3q1w5UJaE0qFUkt5DT2d24at++3xMxzrmJ7V0/f+B/jpZ8nzJskJPeUE/5uYfwfzobyOP8PTJF7e" +
							 	"PU5rjy0u3+ooZ0GCfKOktgU4ZW3nW6i14XiVRSD9C9fe/HLeANzVjAh+wpaVTZutnOhQyTWWnWoyc" +
							 	"QIDAQAB"; 
	
	/* For testing purposes. */
	Map<String, Verifier> publicKeys = new HashMap<String, Verifier>();
	RSAKeyPair X = new RSAKeyPair(privateKey, publicKey);
	Signer sign = new Signer(X.getPrivateKey());
	
	private String roomname;
    
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
		publicKeys.put("eugene", veri);
		setupChatListener();

		longTermKeyPair = new ECMQVKeyPair();
		participants = new TreeMap<>();
		cryptoes = new HashMap<>();
		privateChats = new TreeMap<>();
		LOCK = new Object();
		isInitiator = false;
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

		/* For testing purposes. */
		Verifier veri = new Verifier(X.getPublicKey());
		publicKeys.put("admin", veri);
		publicKeys.put("jiamin", veri);
		setupChatListener();

		longTermKeyPair = new ECMQVKeyPair();
		participants = new TreeMap<>();
		cryptoes = new HashMap<>();
		privateChats = new TreeMap<>();
		LOCK = new Object();
		isInitiator = false;
	}

	/**
	 * Setup the chat listener for private messaging (pairwise).
	 */
	private void setupChatListener() {
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
		this.roomName = muc.getRoom();
		this.messenger = this.createMessenger(publicKeys, sign);
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
					Client.this.roomName = muc.getRoom();
					Client.this.messenger = Client.this.createMessenger(publicKeys, sign);
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
		isInitiator = true;
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
				String messageBody = message.getMessage();
				if (messageBody.equals("$setup"))
				{
					if (!isInitiator)
					{
						try
						{
							authenticate();
						}
						catch (AuthenticationFailureException e)
						{
							System.out.println("Authentication failed.");
						}
					}
				}
				else
				{
					System.out.println(message.getUsername() + ": " + messageBody);
				}
			}
		}, publicKeys, sign, "2xil0x35oH8onjyLeudMlP+5h18r/HZ3drd3WXrqm9I=");
	}
    
    /**
     * Creates a private message chat.
     *
     * @param user the user to chat with
     * @return the chat
     * @throws NotConnectedException
     */
    public Chat createPrivateChat(String user) throws NotConnectedException {
    	String chatDestination = roomName + "/" + user;
    	return muc.createPrivateChat(chatDestination, null);
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
			System.out.println("Authentication failed");
		}
	}

	/**
	 * Do the authentication phase.
	 */
	public void authenticate() throws AuthenticationFailureException
	{
		List<String> occupants = new ArrayList<>(muc.getOccupants());
		Collections.sort(occupants);
		listener = new ChatManagerListener()
		{
			@Override
			public void chatCreated(Chat chat, boolean createdLocally)
			{
				if (!createdLocally)
				{
					try
					{
						System.out.println("Chat created.");
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

		try
		{

			/* Step 1: exchange public keys with all other participants */
			System.out.println("Exchanging public keys with the other participants...");
			exchangePublicKeys(occupants);

			/* Step 2: verify that all participants have the same view of the public keys */
			System.out.println("Verifying public keys...");
			verifyPublicKeys();

			/* Step 3: the first participant(in alphabetic order) generates the root key
			*          and sends it to all other participants */
			System.out.println("Generating root key...");
			sendOrReceiveRootKey();

			/* Step 4: verify that all participants have the same root key */
			System.out.println("Verifying root key...");
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
		String meInChatRoom = roomName + "/" + configFile.get("username");
		int indexOfUser = occupants.indexOf(meInChatRoom) + 1;

		// Participants only establish private chat with participants after him in alphabetical order
		for (; indexOfUser < occupants.size(); ++indexOfUser)
		{
			try
			{
				String user = occupants.get(indexOfUser);
				System.out.println(meInChatRoom + " is creating private chat with " + user);
				Chat chat = createPrivateChat(user);
				System.out.println("Private chat created successfully.");
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

	private void verifyPublicKeys() throws NoSuchAlgorithmException, AuthenticationFailureException, InterruptedException
	{
		MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
		for (Map.Entry<String, String> keyValue : participants.entrySet())
		{
			if (keyValue.getValue() == null)
			{
				throw new AuthenticationFailureException();
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
			throw new AuthenticationFailureException();
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
