package edu.michigan.eecs588.authentication;

import edu.michigan.eecs588.encryption.AESCrypto;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PrivateChat
{
    private Chat chat;
    private final Queue<String> messageQueue;
    private AESCrypto crypto;

    public PrivateChat(MultiUserChat muc, String user)
    {
        chat = muc.createPrivateChat(user, null);
        messageQueue = new ConcurrentLinkedQueue<>();

        chat.addMessageListener(new ChatMessageListener()
        {
            @Override
            public void processMessage(Chat chat, Message message)
            {
                synchronized (messageQueue)
                {
                    messageQueue.add(message.getBody());
                    messageQueue.notify();
                }
            }
        });
    }

    public PrivateChat(Chat chat)
    {
        this.chat = chat;
        messageQueue = new ConcurrentLinkedQueue<>();

        chat.addMessageListener(new ChatMessageListener()
        {
            @Override
            public void processMessage(Chat chat, Message message)
            {
                synchronized (messageQueue)
                {
                    messageQueue.add(message.getBody());
                    messageQueue.notify();
                }
            }
        });
    }

    public void enableEncryption(AESCrypto crypto)
    {
        this.crypto = crypto;
    }

    public void sendMessage(String message) throws SmackException.NotConnectedException
    {
        if (crypto != null)
        {
            message = crypto.encrypt(message);
        }
        chat.sendMessage(message);
    }

    public String nextMessage()
    {
        synchronized (messageQueue)
        {
            if (messageQueue.size() == 0)
            {
                try
                {
                    messageQueue.wait();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
        String message = messageQueue.remove();
        if (crypto != null)
        {
            message = crypto.decrypt(message);
        }

        return message;
    }

    public String getParticipant()
    {
        return chat.getParticipant();
    }

    public void close()
    {
        chat.close();
    }

    @Override
    public String toString()
    {
        return chat.toString();
    }
}
