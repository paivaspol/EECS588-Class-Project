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
    }
}
