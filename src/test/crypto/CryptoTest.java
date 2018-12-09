package crypto;

import extension.crypto.Cipher;
import extension.crypto.CryptoKey;
import extension.crypto.CryptoResult;
import extension.crypto.KeyGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CryptoTest {


    public KeyGenerator keyGenerator;
    public Cipher cipher;

    @Before
    public void init() {
        keyGenerator = new KeyGenerator();
        cipher = new Cipher();
    }

    @Test
    public void enryptThenDecrypt(){

        byte[] salt = keyGenerator.generateSalt();
        CryptoKey key = keyGenerator.generate("unsecureUserPassword", salt);

        byte[] plaintext = "supersecretdata".getBytes();

        CryptoResult ciphertext = cipher.encrypt(key, plaintext);
        byte[] decryptedCiphertext = cipher.decrypt(key, ciphertext.getInitializationVector(), ciphertext.getCiphertext());
        Assert.assertArrayEquals(plaintext, decryptedCiphertext);



    }
}
