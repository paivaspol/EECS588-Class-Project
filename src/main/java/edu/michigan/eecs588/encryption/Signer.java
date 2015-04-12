package edu.michigan.eecs588.encryption;

import org.jivesoftware.smack.util.stringencoder.java7.Base64;

import java.io.UnsupportedEncodingException;
import java.security.*;

public class Signer
{
    private Signature signature;

    public Signer(PrivateKey privateKey)
    {
        try
        {
            signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (InvalidKeyException e)
        {
            e.printStackTrace();
        }
    }

    public String sign(String message)
    {
        try
        {
            byte[] data = message.getBytes("UTF-8");
            signature.update(data);
            return Base64.encodeBytes(signature.sign());
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        catch (SignatureException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
