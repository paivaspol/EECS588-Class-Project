package edu.michigan.eecs588.Messenger;

import edu.michigan.eecs588.Messenger.MMessage;

/**
 * Created by sysofwan on 4/12/15.
 */
public interface MessageReceived {
    void onMessageReceived(MMessage message);
}
