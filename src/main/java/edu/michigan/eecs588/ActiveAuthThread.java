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
    private PrivateChat chat;
    private RSAKeyPair keyPair;

    /**
     * @param client The current chat client
     * @param anotherUser The user the authenticate with
     * @param longTermKeyPair The long term key pair of this user
     * @param keyPair The group specific key pair
     * @throws SmackException.NotConnectedException
     */
    public ActiveAuthThread(Client client, PrivateChat chat, String anotherUser, ECMQVKeyPair longTermKeyPair, RSAKeyPair keyPair)
            throws SmackException.NotConnectedException
    {
        this.client = client;
        this.longTermKeyPair = longTermKeyPair;
        this.anotherUser = anotherUser;
        this.chat = chat;
        this.keyPair = keyPair;
    }

    @Override
    public void run()
    {
        try
        {
            /* Step 1: derive a symmetric key from MQV */
            AESCrypto crypto = deriveMQVKey(longTermKeyPair);

            /* Step 2: Exchange public keys */
            client.getPrinter().println("============Public key exchange starts============");
            client.getPrinter().println("Sending group public key...\n" + keyPair.getPublicKeyAsString());
            chat.sendMessage(crypto.encrypt(keyPair.getPublicKeyAsString()));
            client.getPrinter().println("Group public key sent. Waiting for reply...");
            String publicKeyForThatUser = crypto.decrypt(chat.nextMessage());
            client.getPrinter().println("Public key received.");
            client.getPrinter().println("============Public key exchange ends============\n");

            /* Step 3: Sign the other one's public key for verification */
            client.getPrinter().println("============Signature exchange starts============");
            Signer signer = new Signer(keyPair.getPrivateKey());
            String verificationMessage = crypto.encrypt(signer.sign(publicKeyForThatUser) + "," + publicKeyForThatUser);
            client.getPrinter().println("Sending signature for verification...\n" + signer.sign(publicKeyForThatUser) + "," + publicKeyForThatUser);
            chat.sendMessage(verificationMessage);
            client.getPrinter().println("Signature sent, waiting for reply...");
            String verificationMessageOfThatUser = crypto.decrypt(chat.nextMessage());
            client.getPrinter().println("Signature received.\n" + verificationMessageOfThatUser);
            client.getPrinter().println("============Signature exchange ends============\n");

            /* Step 4: Verify the other one's signature */
            client.getPrinter().println("============Signature verification starts============");
            client.getPrinter().println("Verifying signature...");
            Verifier verifier = new Verifier(publicKeyForThatUser);
            String[] decryptedData = verificationMessageOfThatUser.split(",");
            if (verifier.verify(decryptedData[1], decryptedData[0]))
            {
                client.authDone(anotherUser, publicKeyForThatUser);
            }
            else
            {
                client.authDone(anotherUser, null);
            }
            client.getPrinter().println("============Signature verification ends============\n");
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
            client.getPrinter().println("============MQV starts============");
            client.getPrinter().println("Sending MQV key...");
            chat.sendMessage(MQVPublicKey);
            client.getPrinter().println("MQV Public key sent. Waiting for MQV reply...");
            String message = chat.nextMessage();
            client.getPrinter().println("MQV Public key received.");
            AESCrypto crypto = agreement.doSecondPhase(message);
            client.MQVDone(anotherUser, crypto);
            client.getPrinter().println("============MQV   ends============\n");
            return crypto;

        }
        catch (SmackException.NotConnectedException e)
        {
            throw new RuntimeException(e);
        }
    }
}
