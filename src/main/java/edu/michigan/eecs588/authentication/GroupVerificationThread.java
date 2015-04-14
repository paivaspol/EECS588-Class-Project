package edu.michigan.eecs588.authentication;

import edu.michigan.eecs588.Client;
import edu.michigan.eecs588.authentication.PrivateChat;
import org.jivesoftware.smack.SmackException;

/**
 * This one represents a thread for verification of participants
 * and also generation of the root key
 */
public class GroupVerificationThread implements Runnable
{
    private Client client;
    private PrivateChat chat;
    private String messageForVerification;

    public GroupVerificationThread(Client client, PrivateChat chat, String hashForVerification) throws SmackException.NotConnectedException
    {
        this.client = client;
        this.chat = chat;
        this.messageForVerification = hashForVerification;
    }

    @Override
    public void run()
    {
        try
        {
            chat.sendMessage(messageForVerification);
            client.verify(messageForVerification.equals(chat.nextMessage()));
        }
        catch (SmackException.NotConnectedException e)
        {
            throw new RuntimeException(e);
        }
    }
}
