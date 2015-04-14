package edu.michigan.eecs588.encryption;

import org.bouncycastle.jce.interfaces.MQVPrivateKey;
import org.bouncycastle.jce.interfaces.MQVPublicKey;
import org.bouncycastle.jce.spec.MQVPrivateKeySpec;
import org.bouncycastle.jce.spec.MQVPublicKeySpec;
import org.jivesoftware.smack.util.stringencoder.java7.Base64;

import javax.crypto.KeyAgreement;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class ECMQVKeyAgreement
{
    private  KeyAgreement agreement;

    /**
     * Do the first phase of MQV
     * @param longTermKeyPair The long term ECMQV key pair
     * @return The ephemeral public key which should be sent to the other one
     */
    public String doFirstPhase(ECMQVKeyPair longTermKeyPair)
    {
        ECMQVKeyPair ephemeralKeyPair = new ECMQVKeyPair();
        MQVPrivateKey mqvPrivateKey = new MQVPrivateKeySpec(longTermKeyPair.getPrivateKey(),
                ephemeralKeyPair.getPrivateKey());
        try
        {
            agreement = KeyAgreement.getInstance("ECMQV", "BC");
            agreement.init(mqvPrivateKey);
            return encodeMQVPublicKey(longTermKeyPair.getPublicKey(), ephemeralKeyPair.getPublicKey());
        }
        catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Do the second phase of MQV
     * @param firstPhaseResult The ephemeral public key of the other one
     * @return An AESCrypto with the derived secret
     */
    public AESCrypto doSecondPhase(String firstPhaseResult)
    {
        String[] publicKeys = firstPhaseResult.split(",");
        try
        {
            PublicKey longTermPublicKey =
                    KeyFactory.getInstance("ECMQV", "BC").generatePublic(new X509EncodedKeySpec(Base64.decode(publicKeys[0])));
            PublicKey ephemeralPublicKey =
                    KeyFactory.getInstance("ECMQV", "BC").generatePublic(new X509EncodedKeySpec(Base64.decode(publicKeys[1])));
            MQVPublicKey publicKey = new MQVPublicKeySpec(longTermPublicKey, ephemeralPublicKey);
            agreement.doPhase(publicKey, true);
            return new AESCrypto(agreement.generateSecret());
        }
        catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private  String encodeMQVPublicKey(PublicKey longTermPublicKey, PublicKey ephemeralPublicKey)
    {
        return Base64.encodeBytes(longTermPublicKey.getEncoded()) + "," +
                Base64.encodeBytes(ephemeralPublicKey.getEncoded());
    }
}
