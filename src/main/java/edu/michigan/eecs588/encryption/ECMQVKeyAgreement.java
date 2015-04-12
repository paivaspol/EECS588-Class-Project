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

    public  String doFirstPhase(ECMQVKeyPair longTermKeyPair)
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

    public  String doSecondPhase(String firstPhaseResult)
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
            return Base64.encodeBytes(agreement.generateSecret());
        }
        catch (InvalidKeySpecException e)
        {
            e.printStackTrace();
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

    private  String encodeMQVPublicKey(PublicKey longTermPublicKey, PublicKey ephemeralPublicKey)
    {
        return Base64.encodeBytes(longTermPublicKey.getEncoded()) + "," +
                Base64.encodeBytes(ephemeralPublicKey.getEncoded());
    }
}
