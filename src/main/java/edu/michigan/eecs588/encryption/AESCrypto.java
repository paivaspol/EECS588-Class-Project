package edu.michigan.eecs588.encryption;

import org.jivesoftware.smack.util.stringencoder.java7.Base64;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A class used for encrypting messages given a private key
 */
public class AESCrypto
{
    private static final int KEY_LENGTH = 256;

    private SecretKey secretKey;
    private Cipher encryptCipher;
    private Cipher decryptCipher;
    private MessageDigest sha256Digest;

    public AESCrypto()
    {
        this(KEY_LENGTH);
    }
    
    public AESCrypto(int length)
    {
        try
        {
            sha256Digest = MessageDigest.getInstance("SHA-256");
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(length);
            secretKey = generator.generateKey();
            setCiphers();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }

    public AESCrypto(String secret)
    {
        try
        {
            sha256Digest = MessageDigest.getInstance("SHA-256");
            secretKey = new SecretKeySpec(secret.getBytes(), "AES");
            setCiphers();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }

    private void setCiphers()
    {
        try
        {
            encryptCipher = Cipher.getInstance("AES");
            encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);

            decryptCipher = Cipher.getInstance("AES");
            decryptCipher.init(Cipher.DECRYPT_MODE, secretKey);
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchPaddingException e)
        {
            e.printStackTrace();
        }
        catch (InvalidKeyException e)
        {
            e.printStackTrace();
        }
    }

    public String encrypt(String rawText)
    {
        try
        {
            return Base64.encodeBytes(encryptCipher.doFinal(rawText.getBytes("UTF-8")));
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        catch (BadPaddingException e)
        {
            e.printStackTrace();
        }
        catch (IllegalBlockSizeException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public String decrypt(String cipherText)
    {
        try
        {
            return new String(decryptCipher.doFinal(Base64.decode(cipherText)), "UTF-8");
        }
        catch (IllegalBlockSizeException e)
        {
            e.printStackTrace();
        }
        catch (BadPaddingException e)
        {
            // Decryption fails. Return null
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public String getSecret()
    {
        return Base64.encodeBytes(secretKey.getEncoded());
    }

    public void rollKey()
    {
        secretKey = new SecretKeySpec(sha256Digest.digest(secretKey.getEncoded()), "AES");
        setCiphers();
    }
}
