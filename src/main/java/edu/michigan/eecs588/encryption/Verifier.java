package edu.michigan.eecs588.encryption;

import org.jivesoftware.smack.util.stringencoder.java7.Base64;

import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class Verifier
{
    private Signature signature;

    /**
     * An object used for verifying signatures
     * @param publicKey The public key for verifying the messages
     */
    public Verifier(PublicKey publicKey)
    {
        try
        {
            signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
        }
        catch (NoSuchAlgorithmException | InvalidKeyException e)
        {
            e.printStackTrace();
        }
    }

    /**
     *  An object used for verifying signatures given a public key as a string
     * @param publicKeyString The public key for verifying the messages encoded using Base64
     */
    public Verifier(String publicKeyString)
    {
        try
        {
            PublicKey publicKey =
                    KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(publicKeyString)));
            signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
        }
        catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidKeyException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Verify a signature
     * @param message The message
     * @param cipherText The signature
     * @return True if the verification succeeds, false otherwise
     */
    public boolean verify(String message, String cipherText)
    {
        try
        {
            signature.update(message.getBytes("UTF-8"));
            return signature.verify(Base64.decode(cipherText));
        }
        catch (SignatureException | UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        return false;
    }
}
