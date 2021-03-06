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

    /**
     * Generate a 256-bit AES symmetric key
     */
    public AESCrypto()
    {
        this(KEY_LENGTH);
    }

    /**
     * Generate an AES symmetric key of the specific length
     * @param length Length of the AES key
     */
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

    /**
     * Generate an AES symmetric key given a byte array
     * @param secret A symmetric key as a byte array
     */
    public AESCrypto(byte[] secret)
    {
        try
        {
            sha256Digest = MessageDigest.getInstance("SHA-256");
            secretKey = new SecretKeySpec(secret, "AES");
            setCiphers();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Generate an AES symmetric key given a encoded byte array
     * @param secret A Base64-encoded symmetric key
     */
    public AESCrypto(String secret)
    {
        try
        {
            sha256Digest = MessageDigest.getInstance("SHA-256");
            secretKey = new SecretKeySpec(Base64.decode(secret), "AES");
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
        catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Encrypt a message
     * @param rawText Message to encrypt
     * @return The encrypted message in Base64 encoding
     */
    public String encrypt(String rawText)
    {
        try
        {
            return Base64.encodeBytes(encryptCipher.doFinal(rawText.getBytes("UTF-8")));
        }
        catch (UnsupportedEncodingException | BadPaddingException | IllegalBlockSizeException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Decrypt a message
     * @param cipherText The cipher text to be decrypted
     * @return The original message
     */
    public String decrypt(String cipherText)
    {
        try
        {
            return new String(decryptCipher.doFinal(Base64.decode(cipherText)), "UTF-8");
        }
        catch (IllegalBlockSizeException | UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        catch (BadPaddingException e)
        {
            // Decryption fails. Return null
        }

        return null;
    }

    /**
     * Get the symmetric key as a Base64 encoded string
     * @return
     */
    public String getSecret()
    {
        return Base64.encodeBytes(secretKey.getEncoded());
    }

    /**
     * Roll the symmetric key
     */
    public void rollKey()
    {
        secretKey = new SecretKeySpec(sha256Digest.digest(secretKey.getEncoded()), "AES");
        setCiphers();
    }
}
