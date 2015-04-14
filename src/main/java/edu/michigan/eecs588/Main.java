package edu.michigan.eecs588;

import java.io.IOException;
import java.util.*;

import edu.michigan.eecs588.Messenger.MessageReceived;
import edu.michigan.eecs588.Messenger.Messenger;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jxmpp.util.XmppStringUtils;

import edu.michigan.eecs588.encryption.RSAKeyPair;
import edu.michigan.eecs588.encryption.Signer;
import edu.michigan.eecs588.encryption.Verifier;

public class Main {

	private static final String INVITE = "$invite";
	private static final String CREATE = "$create";
	private static final String SETUP = "$setup";

	private static final String PRIVATE = "$private";
	
	public static void main(String[] args) throws SmackException, IOException, XMPPException {
		Client client = new Client("smack.properties");
		Scanner in = new Scanner(System.in);
		String input;

		Messenger m;
		while (true) {
			client.getPrinter().print();
			input = in.nextLine();
			CommandType commandType = parseInput(input);
			String[] splitted;
			switch (commandType)
			{
				case CREATE:
					splitted = input.split(" ");
					client.createRoom(splitted[1]);
					break;

				case INVITE:
					splitted = input.split(" ");
					client.inviteParticipant(splitted[1]);
					break;

				case SETUP:
					client.setup();
					break;

				case PRIVATE:
					splitted = input.split(" ");
					client.createPrivateChat(splitted[1]);
					break;

				case MESSAGE:
					m = client.getMessenger();
					if (m != null) {
						m.sendMessage(input);
					}
					break;
			}
		}
	}
	
	private static CommandType parseInput(String command) {
    	if (command.startsWith(CREATE)) {
    		return CommandType.CREATE;
    	} else if (command.startsWith(INVITE)) {
    		return CommandType.INVITE;
    	} else if (command.startsWith(SETUP)) {
			return CommandType.SETUP;
		} else if (command.startsWith(PRIVATE)) {
			return CommandType.PRIVATE;
		}
    	return CommandType.MESSAGE;
    }
}
