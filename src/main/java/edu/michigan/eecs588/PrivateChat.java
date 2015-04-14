package edu.michigan.eecs588;

import org.jivesoftware.smack.PacketCollector;
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
    private PacketCollector collector;
    private final Queue<String> messageQueue;

    public PrivateChat(MultiUserChat muc, String user)
    {
        chat = muc.createPrivateChat(user, null);
        collector = chat.createCollector();
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

    public PrivateChat(Chat chat)
    {
        this.chat = chat;
        collector = chat.createCollector();
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
        if (messageQueue.size() == 0)
        {
            System.out.println("\nBlocking for reply...\n");
            return ((Message) collector.nextResultBlockForever()).getBody();
        }
        else
        {
            System.out.println("\nGet message from queue.\n");
            return messageQueue.remove();
        }
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
