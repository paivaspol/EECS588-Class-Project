package edu.michigan.eecs588;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;

/**
 * Entry point for the client for the XMPP.
 */
public class Client {
	
    public static void main(String[] args) throws SmackException, IOException, XMPPException {
        AbstractXMPPConnection connection = createConnection();
		MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
		MultiUserChat muc = manager.getMultiUserChat("myroom@conference.vaspol");
		muc.create("FirstRoom");
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
		owners.add("a@vaspol");
		submitForm.setAnswer("muc#roomconfig_roomowners", owners);
		// Send the completed form (with default values) to the server to configure the room
		muc.sendConfigurationForm(submitForm);
		System.out.println(muc.toString());
        while (true) {}
    }
    
    /**
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
				.setHost("localhost")
				.setPort(5222)
				.setSecurityMode(SecurityMode.disabled)
				.build();
		AbstractXMPPConnection connection = new XMPPTCPConnection(config);
		connection.connect();
		connection.login("a", "password");
		System.out.println("Connected to XMPP server!");
    	return connection;
    }
}
