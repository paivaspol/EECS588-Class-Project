package edu.michigan.eecs588.Messenger;

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;

public class SimpleMessenger implements MessengerInterface {
	private long lastSent;
	private MultiUserChat muc;
	
	public SimpleMessenger(MultiUserChat muc, MessageListener messageListener) {
		this.muc = muc;
		this.muc.addMessageListener(messageListener);
	}
	
	@Override
	public void sendMessage(String message) throws NotConnectedException {
		lastSent = System.currentTimeMillis();
		this.muc.sendMessage(message);
	}

	@Override
	public long lastMessageSentTime()
	{
		return lastSent;
	}


}
