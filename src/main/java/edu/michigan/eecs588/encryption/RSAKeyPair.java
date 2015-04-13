package edu.michigan.eecs588.encryption;

import org.jivesoftware.smack.util.stringencoder.java7.Base64;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

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

    public RSAKeyPair(String privateKeyString, String publicKeyString)
    {
        try
        {
            privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(privateKeyString)));
            publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(publicKeyString)));
        }
        catch (InvalidKeySpecException | NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }

    public PrivateKey getPrivateKey()
    {
        return privateKey;
    }

    public String getPrivateKeyAsString()
    {
        return Base64.encodeBytes(privateKey.getEncoded());
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
