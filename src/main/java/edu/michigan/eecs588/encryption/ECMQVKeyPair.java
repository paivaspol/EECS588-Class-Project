package edu.michigan.eecs588.encryption;

import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
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

            EllipticCurve curve = new EllipticCurve(
                    new ECFieldFp(new BigInteger("883423532389192164791648750360308885314476597252960362792450860609699839")), // q
                    new BigInteger("7fffffffffffffffffffffff7fffffffffff8000000000007ffffffffffc", 16), // a
                    new BigInteger("6b016c3bdcf18941d0d654921475ca71a9db2fb27d1d37796185c2942c0a", 16)); // b

            ECParameterSpec ecSpec = new ECParameterSpec(
                    curve,
                    ECPointUtil.decodePoint(curve, Hex.decode("020ffa963cdca8816ccc33b8642bedf905c3d358573d3f27fbbd3b3cb9aaaf")), // G
                    new BigInteger("883423532389192164791648750360308884807550341691627752275345424702807307"), // n
                    1);
            generator.initialize(ecSpec, new SecureRandom());
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchProviderException e)
        {
            e.printStackTrace();
        }
        catch (InvalidAlgorithmParameterException e)
        {
            e.printStackTrace();
        }
    }

    public ECMQVKeyPair()
    {
        KeyPair pair = generator.generateKeyPair();
        privateKey = pair.getPrivate();
        publicKey  =pair.getPublic();
    }

    public PrivateKey getPrivateKey()
    {
        return privateKey;
    }

    public PublicKey getPublicKey()
    {
        return publicKey;
    }

    public String getPublicKeyAsString()
    {
        return Base64.encodeBytes(publicKey.getEncoded());
    }
}
