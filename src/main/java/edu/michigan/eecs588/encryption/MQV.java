package edu.michigan.eecs588.encryption;

import org.bouncycastle.crypto.agreement.ECMQVBasicAgreement;
import org.bouncycastle.crypto.params.MQVPrivateParameters;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyAgreementSpi;
import org.bouncycastle.jce.interfaces.MQVPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.MQVPrivateKeySpec;

import javax.crypto.KeyAgreement;
import java.security.*;

public class MQV
{
    static
    {
        Security.addProvider(new BouncyCastleProvider());
    }

    MQVPrivateKey mqvPrivateKey;

    public MQV(RSAKeyPair longTermKeyPair)
    {
        RSAKeyPair ephemeralKeyPair = new RSAKeyPair();
        mqvPrivateKey = new MQVPrivateKeySpec(longTermKeyPair.getPrivateKey(),
                ephemeralKeyPair.getPrivateKey(), ephemeralKeyPair.getPublicKey());
        try
        {
            KeyAgreement agreement = KeyAgreement.getInstance("ECMQV", "BC");
            agreement.init(longTermKeyPair.getPrivateKey());
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (NoSuchProviderException e)
        {
            e.printStackTrace();
        }
        catch (InvalidKeyException e)
        {
            e.printStackTrace();
        }
    }

    public int agree()
    {
        return 10;
    }
}
