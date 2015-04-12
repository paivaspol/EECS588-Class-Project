package edu.michigan.eecs588.encryption;

import java.security.*;

public class Verifier
{
    private Signature signature;

    public Verifier(PublicKey publicKey)
    {
        try
        {
            signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
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

    public boolean verify(byte[] data)
    {
        try
        {
            return signature.verify(data);
        }
        catch (SignatureException e)
        {
            e.printStackTrace();
        }
        return false;
    }
}
