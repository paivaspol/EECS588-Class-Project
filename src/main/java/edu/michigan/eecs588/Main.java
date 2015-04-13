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
	private static Queue<Message> queue = new ArrayDeque<Message>();

	public static void main(String[] args) throws SmackException, IOException, XMPPException {
		Client client = new Client();
		Scanner in = new Scanner(System.in);
		String input = "";
		RSAKeyPair X = new RSAKeyPair();
		Signer sign = new Signer(X.getPrivateKey());
		Verifier veri = new Verifier(X.getPublicKey());
		Map<String, Verifier> publicKeys = new HashMap<String, Verifier>();
		Map<String, String> config = client.getConfigFile();
		publicKeys.put(config.get("username"), veri);



		String prompt = "eecs588";

		Messenger m = null;

		while (true) {
			System.out.print(prompt + "> ");
			input = in.nextLine();
			CommandType commandType = parseInput(input);
			if (commandType.equals(CommandType.CREATE)) {
				String[] splitted = input.split(" ");
				client.createRoom(splitted[1]);
				MultiUserChat muc = client.getMultiUserChat();
				prompt = muc.getRoom().toString();
				m = client.createMessenger(publicKeys, sign);
			} else if (commandType.equals(CommandType.INVITE)) {
				String[] splitted = input.split(" ");
				client.inviteParticipant(splitted[1]);
			} else if (m != null) {
				m.sendMessage(input);
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
