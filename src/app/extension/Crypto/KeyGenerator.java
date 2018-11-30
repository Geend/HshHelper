package extension.Crypto;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class KeyGenerator {

    private static SecureRandom secureRandom = new SecureRandom();



    public CryptoKey generate(String password, byte[] salt) {

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, CryptoConstants.PBKDF2_ROUNDS, CryptoConstants.AES_KEY_SIZE);
            SecretKey key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

            return new CryptoKey(key);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Could not derive key from password");
        }
    }

    public CryptoKey generate(byte[] key) {
        return new CryptoKey(new SecretKeySpec(key, "AES"));
    }

    public byte[] generateSalt(){
        byte[] result = new byte[CryptoConstants.SALT_LENGTH];
        secureRandom.nextBytes(result);
        return result;
    }

}
