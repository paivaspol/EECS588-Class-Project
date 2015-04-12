package edu.michigan.eecs588.encryption;

import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.MQVPrivateKeySpec;
import org.bouncycastle.jce.spec.MQVPublicKeySpec;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DHParameterSpec;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.EllipticCurve;
import java.security.spec.X509EncodedKeySpec;

public class DHAgreement implements Runnable {
    byte bob[], alice[];
    boolean doneAlice = false;
    byte[] ciphertext;

    BigInteger aliceP, aliceG;
    int aliceL;

    public synchronized void run() {
        if (!doneAlice) {
            doneAlice = true;
            doAlice();
        }
        else doBob();
    }

    public synchronized void doAlice() {
        try {
            // Step 1:  Alice generates a key pair
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
            kpg.initialize(1024);
            KeyPair kp = kpg.generateKeyPair();

            // Step 2:  Alice sends the public key and the
            // 		Diffie-Hellman key parameters to Bob
            Class dhClass = Class.forName(
                    "javax.crypto.spec.DHParameterSpec");
            DHParameterSpec dhSpec = (
                    (DHPublicKey) kp.getPublic()).getParams();
            aliceG = dhSpec.getG();
            aliceP = dhSpec.getP();
            aliceL = dhSpec.getL();
            alice = kp.getPublic().getEncoded();
            notify();

            // Step 4 part 1:  Alice performs the first phase of the
            //		protocol with her private key
            KeyAgreement ka = KeyAgreement.getInstance("DH");
            ka.init(kp.getPrivate());

            // Step 4 part 2:  Alice performs the second phase of the
            //		protocol with Bob's public key
            while (bob == null) {
                wait();
            }
            KeyFactory kf = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(bob);
            PublicKey pk = kf.generatePublic(x509Spec);
            ka.doPhase(pk, true);

            // Step 4 part 3:  Alice can generate the secret key
            byte secret[] = ka.generateSecret();

            // Step 6:  Alice generates a DES key
            SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
            DESKeySpec desSpec = new DESKeySpec(secret);
            SecretKey key = skf.generateSecret(desSpec);

            // Step 7:  Alice encrypts data with the key and sends
            //		the encrypted data to Bob
            Cipher c = Cipher.getInstance("DES/ECB/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE, key);
            ciphertext = c.doFinal(
                    "Stand and unfold yourself".getBytes());
            notify();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void doBob() {
        try {
            // Step 3:  Bob uses the parameters supplied by Alice
            //		to generate a key pair and sends the public key
            while (alice == null) {
                wait();
            }
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
            DHParameterSpec dhSpec = new DHParameterSpec(
                    aliceP, aliceG, aliceL);
            kpg.initialize(dhSpec);
            KeyPair kp = kpg.generateKeyPair();
            bob = kp.getPublic().getEncoded();
            notify();

            // Step 5 part 1:  Bob uses his private key to perform the
            //		first phase of the protocol
            KeyAgreement ka = KeyAgreement.getInstance("DH");
            ka.init(kp.getPrivate());

            // Step 5 part 2:  Bob uses Alice's public key to perform
            //		the second phase of the protocol.
            KeyFactory kf = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509Spec =
                    new X509EncodedKeySpec(alice);
            PublicKey pk = kf.generatePublic(x509Spec);
            ka.doPhase(pk, true);
            //ka.doPhase(1, k

                    // Step 5 part 3:  Bob generates the secret key
            byte secret[] = ka.generateSecret();

            // Step 6:  Bob generates a DES key
            SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
            DESKeySpec desSpec = new DESKeySpec(secret);
            SecretKey key = skf.generateSecret(desSpec);

            // Step 8:  Bob receives the encrypted text and decrypts it
            Cipher c = Cipher.getInstance("DES/ECB/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, key);
            while (ciphertext == null) {
                wait();
            }
            byte plaintext[] = c.doFinal(ciphertext);
            System.out.println("Bob got the string " +
                    new String(plaintext));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void testECMQV()
            throws Exception
    {
        KeyPairGenerator g = KeyPairGenerator.getInstance("ECMQV", "BC");

        EllipticCurve curve = new EllipticCurve(
                new ECFieldFp(new BigInteger("883423532389192164791648750360308885314476597252960362792450860609699839")), // q
                new BigInteger("7fffffffffffffffffffffff7fffffffffff8000000000007ffffffffffc", 16), // a
                new BigInteger("6b016c3bdcf18941d0d654921475ca71a9db2fb27d1d37796185c2942c0a", 16)); // b

        ECParameterSpec ecSpec = new ECParameterSpec(
                curve,
                ECPointUtil.decodePoint(curve, Hex.decode("020ffa963cdca8816ccc33b8642bedf905c3d358573d3f27fbbd3b3cb9aaaf")), // G
                new BigInteger("883423532389192164791648750360308884807550341691627752275345424702807307"), // n
                1); // h

        g.initialize(ecSpec, new SecureRandom());

        //
        // U side
        //
        KeyPair U1 = g.generateKeyPair();
        KeyPair U2 = g.generateKeyPair();

        KeyAgreement uAgree = KeyAgreement.getInstance("ECMQV", "BC");
        uAgree.init(new MQVPrivateKeySpec(U1.getPrivate(), U2.getPrivate(), U2.getPublic()));

        //
        // V side
        //
        KeyPair V1 = g.generateKeyPair();
        KeyPair V2 = g.generateKeyPair();

        KeyAgreement vAgree = KeyAgreement.getInstance("ECMQV", "BC");
        vAgree.init(new MQVPrivateKeySpec(V1.getPrivate(), V2.getPrivate(), V2.getPublic()));

        //
        // agreement
        //
        uAgree.doPhase(new MQVPublicKeySpec(V1.getPublic(), V2.getPublic()), true);
        vAgree.doPhase(new MQVPublicKeySpec(U1.getPublic(), U2.getPublic()), true);

        BigInteger ux = new BigInteger(uAgree.generateSecret());
        BigInteger vx = new BigInteger(vAgree.generateSecret());

        if (!ux.equals(vx))
        {
            System.out.println("Agreement failed");
        }
        else
        {
            System.out.println(ux);
        }

    }

    public static void main(String args[]) throws Exception
    {
        Security.addProvider(new BouncyCastleProvider());
        DHAgreement test = new DHAgreement();
        test.testECMQV();

        ECMQVKeyAgreement agreement1 = new ECMQVKeyAgreement();
        String publicKey1 = agreement1.doFirstPhase(new ECMQVKeyPair());

        ECMQVKeyAgreement agreement2 = new ECMQVKeyAgreement();
        String publicKey2 = agreement2.doFirstPhase(new ECMQVKeyPair());

        System.out.println(agreement1.doSecondPhase(publicKey2));
        System.out.println(agreement2.doSecondPhase(publicKey1));
    }
}
