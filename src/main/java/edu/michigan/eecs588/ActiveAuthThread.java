package edu.michigan.eecs588;

import edu.michigan.eecs588.encryption.*;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;

/**
 * This class represents a thread for pairwise authentication
 * in which this client initiates the communication
 */
public class ActiveAuthThread implements Runnable
{
    private Client client;
    private ECMQVKeyPair longTermKeyPair;
    private String anotherUser;
    private Chat chat;
    private RSAKeyPair keyPair;
    private String reply;
    private ChatMessageListener listener;

    /**
     * @param client The current chat client
     * @param anotherUser The user the authenticate with
     * @param longTermKeyPair The long term key pair of this user
     * @param keyPair The group specific key pair
     * @throws SmackException.NotConnectedException
     */
    public ActiveAuthThread(Client client, Chat chat, String anotherUser, ECMQVKeyPair longTermKeyPair, RSAKeyPair keyPair)
            throws SmackException.NotConnectedException
    {
        this.client = client;
        this.longTermKeyPair = longTermKeyPair;
        this.anotherUser = anotherUser;
        this.chat = chat;
        this.keyPair = keyPair;
        reply = null;
        listener = new ChatMessageListener()
        {
            @Override
            public void processMessage(Chat chat, Message message)
            {
                reply = message.getBody();
                ActiveAuthThread.this.notify();
            }
        };
        chat.addMessageListener(listener);
    }

    @Override
    public void run()
    {
        try
        {
            AESCrypto crypto = deriveMQVKey(longTermKeyPair);
            chat.sendMessage(crypto.encrypt(keyPair.getPublicKeyAsString()));
            String publicKeyForThatUser = waitForReply();
            Signer signer = new Signer(keyPair.getPrivateKey());
            String verificationMessage = crypto.encrypt(signer.sign(publicKeyForThatUser) + "," + publicKeyForThatUser);
            chat.sendMessage(verificationMessage);
            String verificationMessageOfThatUser = waitForReply();
            Verifier verifier = new Verifier(publicKeyForThatUser);
            String[] decryptedData = crypto.decrypt(verificationMessageOfThatUser).split(",");
            if (verifier.verify(decryptedData[1], decryptedData[0]))
            {
                client.authDone(anotherUser, publicKeyForThatUser);
            }
            else
            {
                client.authDone(anotherUser, null);
            }
            chat.removeMessageListener(listener);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        catch (SmackException.NotConnectedException e)
        {
            throw new RuntimeException(e);
        }
    }

    private AESCrypto deriveMQVKey(ECMQVKeyPair longTermKeyPair) throws InterruptedException
    {
        ECMQVKeyAgreement agreement = new ECMQVKeyAgreement();
        String MQVPublicKey = agreement.doFirstPhase(longTermKeyPair);

        try
        {
            chat.sendMessage(MQVPublicKey);
            AESCrypto crypto = agreement.doSecondPhase(waitForReply());
            client.MQVDone(anotherUser, crypto);
            return crypto;

        }
        catch (SmackException.NotConnectedException e)
        {
            throw new RuntimeException(e);
        }
    }

    private String waitForReply()
    {
        while (reply == null)
        {
            synchronized (this)
            {
                try
                {
                    this.wait();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
        String message = reply;
        reply = null;
        return message;
    }
}
