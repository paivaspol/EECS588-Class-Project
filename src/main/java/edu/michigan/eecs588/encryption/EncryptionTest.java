package edu.michigan.eecs588.encryption;

public class EncryptionTest
{
    public static void main(String[] args)
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
    }
}
