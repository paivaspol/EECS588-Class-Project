package edu.michigan.eecs588.encryption;

import org.jivesoftware.smack.util.stringencoder.java7.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Used for doing SHA256 hashing
 */
public class SHA256Hash
{
    private static MessageDigest sha256Digest;
    static
    {
        try
        {
            sha256Digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }

    public static String hash(byte[] data)
    {
        return Base64.encodeBytes(sha256Digest.digest(data));
    }
}
