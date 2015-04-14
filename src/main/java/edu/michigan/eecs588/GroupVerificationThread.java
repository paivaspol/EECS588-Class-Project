package edu.michigan.eecs588;

import edu.michigan.eecs588.encryption.AESCrypto;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;

/**
 * This one represents a thread for verification of participants
 * and also generation of the root key
 */
public class GroupVerificationThread implements Runnable
{
    private Client client;
    private PrivateChat chat;
    private String messageForVerification;

    public GroupVerificationThread(Client client, PrivateChat chat, String hashForVerification, AESCrypto crypto) throws SmackException.NotConnectedException
    {
        this.client = client;
        this.chat = chat;
        this.messageForVerification = crypto.encrypt(hashForVerification);
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
