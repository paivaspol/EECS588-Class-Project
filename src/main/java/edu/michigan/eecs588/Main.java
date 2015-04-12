package edu.michigan.eecs588;

import java.io.IOException;
import java.util.Scanner;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jxmpp.util.XmppStringUtils;

public class Main {

	private static final String INVITE = "$invite";
	private static final String CREATE = "$create";
	
	public static void main(String[] args) throws SmackException, IOException, XMPPException {
		Client client = new Client();
		Scanner in = new Scanner(System.in);
		String input = "";
		String prompt = "eecs588";
		while (true) {
			System.out.print(prompt + "> ");
			input = in.nextLine();
			CommandType commandType = parseInput(input);
			if (commandType.equals(CommandType.CREATE)) {
				String[] splitted = input.split(" ");
				client.createRoom(splitted[1]);
				MultiUserChat muc = client.getMultiUserChat();
				prompt = muc.getRoom().toString();
			} else if (commandType.equals(CommandType.INVITE)) {
				String[] splitted = input.split(" ");
				client.inviteParticipant(splitted[1]);
			} else {
				MultiUserChat muc = client.getMultiUserChat();
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
	
}
