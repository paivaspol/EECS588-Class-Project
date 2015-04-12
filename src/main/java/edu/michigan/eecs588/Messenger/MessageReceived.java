package edu.michigan.eecs588.Messenger;

import org.jivesoftware.smack.packet.Message;

/**
 * Created by sysofwan on 4/12/15.
 */
public interface MessageReceived {
    void onMessageReceived(Message message);
}
