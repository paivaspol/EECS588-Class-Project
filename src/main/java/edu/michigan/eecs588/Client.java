package edu.michigan.eecs588;

import java.io.IOException;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;


/**
 * Entry point for the client for the XMPP.
 */
public class Client {
	
    public static void main(String[] args) throws SmackException, IOException, XMPPException {
        AbstractXMPPConnection connection = createConnection();
		MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
		MultiUserChat muc = manager.getMultiUserChat("test@example.com");
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
				.setServiceName("example.com")
				.setUsernameAndPassword("me", "1234")
				.setHost("localhost")
				.setPort(5222)
				.setSecurityMode(SecurityMode.disabled)
				.build();
		AbstractXMPPConnection connection = new XMPPTCPConnection(config);
		try {
			connection.connect();
			connection.login("me", "1234");
			System.out.println("Connected to XMPP server!");
		} catch (SmackException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XMPPException e) {
			e.printStackTrace();
		}

    	return connection;
    }
}
