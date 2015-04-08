package edu.michigan.eecs588;

import java.io.IOException;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

/**
 * Entry point for the client for the XMPP.
 */
public class Client {
	
    public static void main(String[] args) throws SmackException, IOException, XMPPException {
        createConnection();
    }
    
    private static void createConnection() throws SmackException, IOException, XMPPException {
    	XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
    			  .setUsernameAndPassword("d", "password")
    			  .setServiceName("vaspol")
    			  .setHost("localhost")
    			  .setPort(5222)
    			  .build();
    	AbstractXMPPConnection connection = new XMPPTCPConnection(config);
    	connection.connect();
    }
}
