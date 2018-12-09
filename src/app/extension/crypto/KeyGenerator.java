package extension.crypto;

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
            SecretKeyFactory factory = SecretKeyFactory.getInstance(CryptoConstants.KEY_DERIVATION_FUNCTION_NAME);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, CryptoConstants.PBKDF2_ROUNDS, CryptoConstants.AES_KEY_SIZE_BIT);
            SecretKey key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), CryptoConstants.CIPHER_NAME);

            return new CryptoKey(key);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Could not derive key from password");
        }
    }

    public CryptoKey generate(byte[] key) {
        return new CryptoKey(new SecretKeySpec(key, CryptoConstants.CIPHER_NAME));
    }

    public byte[] generateSalt(){
        byte[] result = new byte[CryptoConstants.SALT_LENGTH_BYTE];
        secureRandom.nextBytes(result);
        return result;
    }

}
