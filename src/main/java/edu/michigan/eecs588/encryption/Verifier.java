package edu.michigan.eecs588.encryption;

import org.jivesoftware.smack.util.stringencoder.java7.Base64;

import java.io.UnsupportedEncodingException;
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
                    KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(publicKeyString)));
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

    public boolean verify(String message, String cipherText)
    {
        try
        {
            signature.update(message.getBytes("UTF-8"));
            return signature.verify(Base64.decode(cipherText));
        }
        catch (SignatureException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        return false;
    }
}
