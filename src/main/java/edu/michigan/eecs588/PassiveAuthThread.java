package edu.michigan.eecs588;

import edu.michigan.eecs588.encryption.*;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;

/**
 * This class represents a thread for pairwise authentication
 * in which the other client initiates the communication
 */
public class PassiveAuthThread implements Runnable
{
    private Client client;
    private ECMQVKeyPair longTermKeyPair;
    private String anotherUser;
    private PrivateChat chat;
    private RSAKeyPair keyPair;
//    private String reply;
//    private final Object LOCK;

    /**
     * @param client The current chat client
     * @param anotherUser The user the authenticate with
     * @param longTermKeyPair The long term key pair of this user
     * @param keyPair The group specific key pair
     * @throws SmackException.NotConnectedException
     */
    public PassiveAuthThread(Client client, PrivateChat chat, String anotherUser, ECMQVKeyPair longTermKeyPair, RSAKeyPair keyPair)
            throws SmackException.NotConnectedException
    {
        this.client = client;
        this.longTermKeyPair = longTermKeyPair;
        this.anotherUser = anotherUser;
        this.chat = chat;
        this.keyPair = keyPair;
//        reply = null;
//        LOCK = new Object();
//        chat.addMessageListener(new ChatMessageListener()
//        {
//            @Override
//            public void processMessage(Chat chat, Message message)
//            {
//                synchronized (LOCK)
//                {
//                    reply = message.getBody();
//                    LOCK.notify();
//                }
//            }
//        });
    }

    @Override
    public void run()
    {
        try
        {
            AESCrypto crypto = deriveMQVKey(longTermKeyPair);
            client.getPrinter().println("Waiting for public key of another thread...");
            String publicKeyForThatUser = crypto.decrypt(waitForReply());
            client.getPrinter().println("Public key received. Sending my public key...");
            chat.sendMessage(crypto.encrypt(keyPair.getPublicKeyAsString()));
            client.getPrinter().println("Public key sent.");
            Signer signer = new Signer(keyPair.getPrivateKey());
            String verificationMessage = crypto.encrypt(signer.sign(publicKeyForThatUser) + "," + publicKeyForThatUser);
            client.getPrinter().println("Waiting for his signature...");
            String verificationMessageOfThatUser = waitForReply();
            client.getPrinter().println("Signature received. Sending my signature...");
            chat.sendMessage(verificationMessage);
            client.getPrinter().println("Signature sent.");
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
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        catch (SmackException.NotConnectedException e)
        {
            throw new RuntimeException(e);
        }
        chat.close();
    }

    private AESCrypto deriveMQVKey(ECMQVKeyPair longTermKeyPair) throws InterruptedException
    {
        ECMQVKeyAgreement agreement = new ECMQVKeyAgreement();
        String MQVPublicKey = agreement.doFirstPhase(longTermKeyPair);

        try
        {
            client.getPrinter().println("Waiting for MQV reply from the other thread...");
            AESCrypto crypto = agreement.doSecondPhase(waitForReply());
            client.getPrinter().println("MQV done. Sending MQV reply to the other thread...");
            chat.sendMessage(MQVPublicKey);
            client.getPrinter().println("MQV reply sent.");
            return crypto;

        }
        catch (SmackException.NotConnectedException e)
        {
            throw new RuntimeException(e);
        }
    }

    private String waitForReply()
    {
        return chat.nextMessage();
//        while (reply == null)
//        {
//            synchronized (LOCK)
//            {
//                try
//                {
//                    LOCK.wait();
//                }
//                catch (InterruptedException e)
//                {
//                    e.printStackTrace();
//                }
//            }
//        }
//        String message = reply;
//        reply = null;
//        return message;
    }
}
