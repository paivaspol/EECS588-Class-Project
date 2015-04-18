package edu.michigan.eecs588.Messenger;

import org.jivesoftware.smack.SmackException;

public interface MessengerInterface {

	public abstract void sendMessage(String message)
			throws SmackException.NotConnectedException;

}