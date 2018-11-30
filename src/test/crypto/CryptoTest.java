package crypto;

import extension.Crypto.Cipher;
import extension.Crypto.CryptoKey;
import extension.Crypto.CryptoResult;
import extension.Crypto.KeyGenerator;
import org.junit.*;
import play.test.Helpers;

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

        CryptoKey key = keyGenerator.generate("1234", "abcd");
        byte[] plaintext = "supersecretdata".getBytes();

        CryptoResult ciphertext = cipher.encrypt(key, plaintext);
        byte[] decryptedCiphertext = cipher.decrypt(key, ciphertext.getInitializationVector(), ciphertext.getCiphertext());
        Assert.assertArrayEquals(plaintext, decryptedCiphertext);



    }
}
