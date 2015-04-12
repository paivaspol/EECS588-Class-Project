package edu.michigan.eecs588.encryption;

import org.bouncycastle.jce.interfaces.MQVPrivateKey;
import org.bouncycastle.jce.interfaces.MQVPublicKey;
import org.bouncycastle.jce.spec.MQVPrivateKeySpec;
import org.bouncycastle.jce.spec.MQVPublicKeySpec;

import javax.crypto.KeyAgreement;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class MQVKeyAgreement
{
    KeyAgreement agreement;

    public MQVKeyAgreement(ECMQVKeyPair longTermKeyPair)
    {
        ECMQVKeyPair ephemeralKeyPair = new ECMQVKeyPair();
        MQVPrivateKey mqvPrivateKey = new MQVPrivateKeySpec(longTermKeyPair.getPrivateKey(),
                ephemeralKeyPair.getPrivateKey());
        MQVPublicKey mqvPublicKey = new MQVPublicKeySpec(longTermKeyPair.getPublicKey(),
                ephemeralKeyPair.getPublicKey());
        try
        {
            KeyAgreement agreement = KeyAgreement.getInstance("ECMQV", "BC");
            agreement.init(mqvPrivateKey);
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchProviderException e)
        {
            e.printStackTrace();
        }
        catch (InvalidKeyException e)
        {
            e.printStackTrace();
        }
    }

    public String doFirstPhase(ECMQVKeyPair longTermKeyPair)
    {
        ECMQVKeyPair ephemeralKeyPair = new ECMQVKeyPair();
        MQVPrivateKey mqvPrivateKey = new MQVPrivateKeySpec(longTermKeyPair.getPrivateKey(),
                ephemeralKeyPair.getPrivateKey());
        MQVPublicKey mqvPublicKey = new MQVPublicKeySpec(longTermKeyPair.getPublicKey(),
                ephemeralKeyPair.getPublicKey());
        try
        {
            agreement = KeyAgreement.getInstance("ECMQV", "BC");
            agreement.init(mqvPrivateKey);


        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchProviderException e)
        {
            e.printStackTrace();
        }
        catch (InvalidKeyException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private String encodeMQVPublicKey(MQVPublicKey longTermPublicKey, MQVPublicKey ephemeralPublicKey)
    {
        //return Base64.encode(longTermPublicKey.)
        return null;
    }
}
