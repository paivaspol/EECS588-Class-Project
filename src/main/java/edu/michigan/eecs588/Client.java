package edu.michigan.eecs588;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Map;

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
import org.jxmpp.util.XmppStringUtils;

/**
 * Entry point for the client for the XMPP.
 */
public class Client {
	
	private static final String INVITE = "$invite";
	private static final String CREATE = "$create";
	
    public static void main(String[] args) throws SmackException, IOException, XMPPException {
		Scanner in = new Scanner(System.in);
		String input = "";
		String prompt = "eecs588";
    	Map<String, String> configFile = ConfigFileReader.getConfigValues();
        AbstractXMPPConnection connection = createConnectionAndLogin(configFile);
		addInvitationListener(connection);
		MultiUserChat muc = null;
		while (true) {
			System.out.print(prompt + "> ");
			input = in.nextLine();
			CommandType commandType = parseInput(input);
			if (commandType.equals(CommandType.CREATE)) {
				String[] splitted = input.split(" ");
				muc = createRoom(connection, configFile, splitted[1]);
				prompt = muc.getNickname();
			} else if (commandType.equals(CommandType.INVITE)) {
				String[] splitted = input.split(" ");
				inviteParticipant(muc, splitted[1]);
			} else {
				muc.sendMessage(input);
				Message message = muc.nextMessage();
				System.out.println(XmppStringUtils.parseResource(message.getFrom()) + " says: " + message.getBody());
			}
		}
    }
    
    private static CommandType parseInput(String command) {
    	if (command.startsWith(CREATE)) {
    		return CommandType.CREATE;
    	} else if (command.startsWith(INVITE)) {
    		return CommandType.INVITE;
    	}
    	return CommandType.MESSAGE;
    }
    
    /**
     * Connects to the xmpp server.
     * 
     * @throws SmackException
     * @throws IOException
     * @throws XMPPException
     */
    private static AbstractXMPPConnection createConnectionAndLogin(Map<String, String> configFile) throws SmackException, IOException, XMPPException {
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
    private static MultiUserChat createRoom(AbstractXMPPConnection connection, Map<String, String> configFile, String roomname) throws XMPPErrorException, SmackException {
    	MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
    	String multiChatService = configFile.get("multiUserChatService");
		MultiUserChat muc = manager.getMultiUserChat(roomname + "@" + multiChatService);
		muc.create(configFile.get("username"));

		System.out.println("Welcome to " + muc.getRoom());
		System.out.println("Type your message and press Enter to send.");

		List<String> owners = new ArrayList<>();
		owners.add(configFile.get("username") + "@" + configFile.get("serviceName"));

		return muc;
    }
    
    /**
     * Adds an invitation listener to the connection
     * 
     * @param connection the connection to add to
     */
    private static void addInvitationListener(AbstractXMPPConnection connection) {
    	MultiUserChatManager.getInstanceFor(connection).addInvitationListener(new InvitationListener() {
			@Override
			public void invitationReceived(XMPPConnection connection, MultiUserChat muc,
					String room, String inviter, String reason, Message password) {
				try {
					System.out.println("Received an invitation to join: " + room);
					muc.join(room);
					System.out.println("Joined: " + room);
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
    private static void inviteParticipant(MultiUserChat muc, String user) throws NotConnectedException {
    	System.out.println("Inviting " + user);
    	muc.invite(user, "I love you");
    }
}
