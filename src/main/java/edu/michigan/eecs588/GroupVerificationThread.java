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
    private Chat chat;
    private String messageForVerification;
    private ChatMessageListener listener;

    public GroupVerificationThread(Client client, Chat chat, String hashForVerification, AESCrypto crypto) throws SmackException.NotConnectedException
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
            listener = new ChatMessageListener()
            {
                @Override
                public void processMessage(Chat chat, Message message)
                {
                    String reply = message.getBody();
                    client.verify(messageForVerification.equals(reply));
                    chat.removeMessageListener(listener);
                }
            };
            chat.addMessageListener(listener);
            chat.sendMessage(messageForVerification);
        }
        catch (SmackException.NotConnectedException e)
        {
            throw new RuntimeException(e);
        }
    }
}
