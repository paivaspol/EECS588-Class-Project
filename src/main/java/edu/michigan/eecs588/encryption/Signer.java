package edu.michigan.eecs588.encryption;

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

    public byte[] sign(String message)
    {
        try
        {
            byte[] data = message.getBytes("UTF-8");
            signature.update(data);
            return signature.sign();
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
