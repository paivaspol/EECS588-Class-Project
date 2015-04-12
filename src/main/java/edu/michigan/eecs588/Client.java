package edu.michigan.eecs588;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jxmpp.util.XmppStringUtils;

/**
 * Entry point for the client for the XMPP.
 */
public class Client {
	
    public static void main(String[] args) throws SmackException, IOException, XMPPException {
		Scanner in = new Scanner(System.in);
		String input = "";
        AbstractXMPPConnection connection = createConnection();
		MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
		MultiUserChat muc = manager.getMultiUserChat("myroom@conference.vaspol");
//		System.out.println(Arrays.toString(manager.getHostedRooms("vaspol").toArray()));
		muc.createOrJoin("vanblaze");
		// Get the the room's configuration form
//		Form form = muc.getConfigurationForm();
		// Create a new form to submit based on the original form
//		Form submitForm = form.createAnswerForm();
		// Add default answers to the form to submit
//		for (FormField field : form.getFields()) {
//			if (!FormField.Type.hidden.equals(field.getType()) && field.getVariable() != null) {
				// Sets the default value as the answer
//				submitForm.setDefaultAnswer(field.getVariable());
//			}
//		}
		// Sets the new owner of the room

//		submitForm.setAnswer("muc#roomconfig_roomowners", owners);
		// Send the completed form (with default values) to the server to configure the room
//		muc.sendConfigurationForm(submitForm);
//		System.out.println(muc.toString());
		System.out.println("Welcome to " + muc.getRoom());
		System.out.println("Type your message and press Enter to send.");
		while (true) {
			input = in.nextLine();
			muc.sendMessage(input);

			Message message = muc.nextMessage();
			System.out.println(XmppStringUtils.parseResource(message.getFrom()) + ": " + message.getBody());
		}
    }
    
    /** 35.2.91.44
     * Connects to the xmpp server.
     * 
     * @throws SmackException
     * @throws IOException
     * @throws XMPPException
     */
    private static AbstractXMPPConnection createConnection() throws SmackException, IOException, XMPPException {
		XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
				.setServiceName("vaspol")
				.setUsernameAndPassword("a", "password")
				.setHost("35.2.91.44")
				.setPort(5222)
				.setSecurityMode(SecurityMode.disabled)
				.build();
		AbstractXMPPConnection connection = new XMPPTCPConnection(config);
		connection.connect();
		connection.login("eugene", "password");
		System.out.println("Connected to XMPP server " + connection.getHost() + "!");
    	return connection;
    }
}
