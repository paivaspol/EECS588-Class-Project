package edu.michigan.eecs588.Messenger;

import org.jxmpp.util.XmppStringUtils;

/**
 * Created by sysofwan on 4/12/15.
 */
public class MMessage {

    private String message;
    private String from;

    public MMessage(String message, String from) {
        this.message = message;
        this.from = from;
    }

    public String getMessage() {
        return message;
    }

    public String getUsername() {
        return XmppStringUtils.parseResource(from);
    }
}
