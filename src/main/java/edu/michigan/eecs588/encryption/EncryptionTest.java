package edu.michigan.eecs588.encryption;

import org.jivesoftware.smack.util.stringencoder.java7.Base64;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class EncryptionTest
{
    public static void main(String[] args) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException
    {
        AESCrypto crypto = new AESCrypto();
        String message = "Hello, world!";

        String cipherText = crypto.encrypt(message);
        System.out.println("Cipher text: \n" + cipherText);
        System.out.println("Original text: \n" + crypto.decrypt(cipherText));
        System.out.println("Secret: \n" + crypto.getSecret());

        cipherText = crypto.encrypt(message);
        System.out.println("Cipher text: \n" + cipherText);
        System.out.println("Original text: \n" + crypto.decrypt(cipherText));
        System.out.println("Secret: \n" + crypto.getSecret());

        crypto.rollKey();

        cipherText = crypto.encrypt(message);
        System.out.println("Cipher text: \n" + cipherText);
        System.out.println("Original text: \n" + crypto.decrypt(cipherText));
        System.out.println("Secret: \n" + crypto.getSecret());

        RSAKeyPair keyPair = new RSAKeyPair();

        String privateKey = keyPair.getPrivateKeyAsString();
        String publicKey = keyPair.getPublicKeyAsString();
        System.out.println("Private key is:\n" + keyPair.getPrivateKeyAsString() + "\n");
        System.out.println("Public key is:\n" + keyPair.getPublicKeyAsString() + "\n");

        keyPair = new RSAKeyPair(privateKey, publicKey);

        Signer signer1 = new Signer(keyPair.getPrivateKey());
        Verifier verifier1 = new Verifier(keyPair.getPublicKey());

        keyPair = new RSAKeyPair();
        Signer signer2 = new Signer(keyPair.getPrivateKey());
        Verifier verifier2 = new Verifier(keyPair.getPublicKeyAsString());

        System.out.println(verifier1.verify(message, signer1.sign(message)));
        System.out.println(verifier1.verify(message, signer2.sign(message)));
        System.out.println(verifier2.verify(message, signer1.sign(message)));
        System.out.println(verifier2.verify(message, signer2.sign(message)));

        ECMQVKeyPair ecmqvKeyPair = new ECMQVKeyPair();
        System.out.println(ecmqvKeyPair.getPublicKeyAsString());

        PublicKey longTermPublicKey =
                KeyFactory.getInstance("ECMQV", "BC").generatePublic(new X509EncodedKeySpec(Base64.decode(ecmqvKeyPair.getPublicKeyAsString())));
        System.out.println(longTermPublicKey.getAlgorithm());
    }
}
