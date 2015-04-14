package edu.michigan.eecs588.encryption;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.util.encoders.Hex;
import org.jivesoftware.smack.util.stringencoder.java7.Base64;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.EllipticCurve;

public class ECMQVKeyPair
{
    private static KeyPairGenerator generator;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    static
    {
        initialize();
    }

    private static void initialize()
    {
        try
        {
            Security.addProvider(new BouncyCastleProvider());
            generator = KeyPairGenerator.getInstance("ECMQV", "BC");

            ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1");
            generator.initialize(ecSpec, new SecureRandom());
        }
        catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Generate an ECMQV key pair
     */
    public ECMQVKeyPair()
    {
        KeyPair pair = generator.generateKeyPair();
        privateKey = pair.getPrivate();
        publicKey  =pair.getPublic();
    }

    /**
     * Get the private key
     * @return the private key
     */
    public PrivateKey getPrivateKey()
    {
        return privateKey;
    }

    /**
     * Get the public key
     * @return the public key
     */
    public PublicKey getPublicKey()
    {
        return publicKey;
    }

    /**
     * Get the public key as a Base64 encoded string
     * @return the public key as a string
     */
    public String getPublicKeyAsString()
    {
        return Base64.encodeBytes(publicKey.getEncoded());
    }
}
