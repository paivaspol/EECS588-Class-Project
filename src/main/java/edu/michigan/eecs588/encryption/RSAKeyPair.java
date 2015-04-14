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

    /**
     * A RSA key pair of default length(2048)
     */
    public RSAKeyPair()
    {
        this(KEY_LENGTH);
    }

    /**
     * A RSA key pair of the given length
     * @param length
     */
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

    /**
     * A RSA key pair given a private key and a public key
     * @param privateKeyString private key as a Base64 encoded string
     * @param publicKeyString public key as a Base64 encoded string
     */
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

    /**
     * Get the private key
     * @return the private key
     */
    public PrivateKey getPrivateKey()
    {
        return privateKey;
    }

    /**
     * Get the private key as a Base64 encoded string
     * @return the private key as a string
     */
    public String getPrivateKeyAsString()
    {
        return Base64.encodeBytes(privateKey.getEncoded());
    }

    /**
     * Get the public key
     * @return the public key
     */
    public PublicKey getPublicKey()
    {
        return publicKey;
    }

    /**
     * Get the public key as a Base64 encoded string
     * @return the public key as a string
     */
    public String getPublicKeyAsString()
    {
        return Base64.encodeBytes(publicKey.getEncoded());
    }
}
