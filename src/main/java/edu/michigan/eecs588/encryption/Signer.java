package edu.michigan.eecs588.encryption;

import org.jivesoftware.smack.util.stringencoder.java7.Base64;

import java.io.UnsupportedEncodingException;
import java.security.*;

public class Signer
{
    private Signature signature;

    /**
     * An object used for signing data given a private key
     * @param privateKey the private of the key pair
     */
    public Signer(PrivateKey privateKey)
    {
        try
        {
            signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
        }
        catch (NoSuchAlgorithmException | InvalidKeyException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Sign the message with the private key
     * @param message The message to be signed
     * @return A signature corresponding to the given message
     */
    public String sign(String message)
    {
        try
        {
            byte[] data = message.getBytes("UTF-8");
            signature.update(data);
            return Base64.encodeBytes(signature.sign());
        }
        catch (UnsupportedEncodingException | SignatureException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
