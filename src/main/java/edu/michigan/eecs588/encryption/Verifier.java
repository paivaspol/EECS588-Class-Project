package edu.michigan.eecs588.encryption;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

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


    public Verifier(String publicKeyString)
    {
        try
        {
            PublicKey publicKey =
                    KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyString.getBytes()));
            signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
        }
        catch (InvalidKeySpecException e)
        {
            e.printStackTrace();
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

    public boolean verify(String cipherText)
    {
        try
        {
            return signature.verify(cipherText.getBytes());
        }
        catch (SignatureException e)
        {
            e.printStackTrace();
        }

        return false;
    }
}
