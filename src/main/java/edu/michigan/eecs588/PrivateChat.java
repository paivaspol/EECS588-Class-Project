package edu.michigan.eecs588;

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
//                System.out.println("Message received: \n" + message.getBody());
                synchronized (messageQueue)
                {
                    messageQueue.add(message.getBody());
                    messageQueue.notify();
                }
            }
        });
    }

    public void sendMessage(String message) throws SmackException.NotConnectedException
    {
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
        return messageQueue.remove();

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
