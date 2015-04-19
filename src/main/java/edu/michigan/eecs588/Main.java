package edu.michigan.eecs588;

import java.io.IOException;
import java.util.*;

import edu.michigan.eecs588.Messenger.MessageReceived;
import edu.michigan.eecs588.Messenger.MessengerInterface;

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
		Calendar start = Calendar.getInstance();
		start.set(Calendar.YEAR, 2014);
		start.set(Calendar.MONTH, 1);
		start.set(Calendar.DAY_OF_MONTH, 1);
		start.set(Calendar.HOUR_OF_DAY, 0);
		start.set(Calendar.MINUTE, 0);
		start.set(Calendar.SECOND, 0);
		Calendar end = Calendar.getInstance();
		end.set(Calendar.YEAR, 2019);
		end.set(Calendar.MONTH, 12);
		end.set(Calendar.DAY_OF_MONTH, 31);
		end.set(Calendar.HOUR_OF_DAY, 23);
		end.set(Calendar.MINUTE, 59);
		end.set(Calendar.SECOND, 59);

		System.out.println("Difference is " + end.getTimeInMillis() / 1000);

		Client client = new Client("user0.properties");
		Scanner in = new Scanner(System.in);
		String input;

		MessengerInterface m;
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
					for (int index = 1; index < 20; ++index)
					{
						client.inviteParticipant("user" + index);
					}
					break;

				case SETUP:
					long before = System.currentTimeMillis();
					client.setup();
					long duration = System.currentTimeMillis() - before;
					client.getPrinter().println("Set up time is " + duration);
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
