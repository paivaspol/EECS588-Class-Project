package edu.michigan.eecs588.Messenger;

import org.jivesoftware.smack.SmackException;

public interface MessengerInterface {

	void sendMessage(String message)
			throws SmackException.NotConnectedException;

	long lastMessageSentTime();
}