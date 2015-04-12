package edu.michigan.eecs588.encryption;

import org.jivesoftware.smack.util.stringencoder.java7.Base64;

import java.security.*;

public class RSAKeyPair
{
    private static final int KEY_LENGTH = 2048;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    public RSAKeyPair()
    {
        this(KEY_LENGTH);
    }

    public RSAKeyPair(int length)
    {
        KeyPairGenerator keyPairGenerator;
        try
        {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(length);
            KeyPair pair = keyPairGenerator.generateKeyPair();
            privateKey = pair.getPrivate();
            publicKey = pair.getPublic();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }

    public PrivateKey getPrivateKey()
    {
        return privateKey;
    }

    public PublicKey getPublicKey()
    {
        return publicKey;
    }

    public String getPublicKeyAsString()
    {
        return Base64.encodeBytes(publicKey.getEncoded());
    }
}
