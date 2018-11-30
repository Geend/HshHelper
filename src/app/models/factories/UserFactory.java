package models.factories;

import extension.Crypto.Cipher;
import extension.Crypto.CryptoKey;
import extension.Crypto.CryptoResult;
import extension.Crypto.KeyGenerator;
import extension.HashHelper;
import extension.RandomDataGenerator;
import models.User;

import javax.inject.Inject;

public class UserFactory {
    private HashHelper hashHelper;
    private RandomDataGenerator randomDataGenerator;
    private KeyGenerator keyGenerator;
    private Cipher cipher;

    @Inject
    public UserFactory(HashHelper hashHelper, RandomDataGenerator randomDataGenerator, KeyGenerator keyGenerator, Cipher cipher) {
        this.hashHelper = hashHelper;
        this.randomDataGenerator = randomDataGenerator;
        this.keyGenerator = keyGenerator;
        this.cipher = cipher;
    }

    public User CreateUser(String username, String email, String plaintextPassword, Boolean passwordResetRequired, Long quotaLimit) {
        User u = new User(
            username,
            email,
            hashHelper.hashPassword(plaintextPassword),
            passwordResetRequired,
            quotaLimit
        );

        u.setCryptoSalt(keyGenerator.generateSalt());
        CryptoKey ck = keyGenerator.generate(plaintextPassword, u.getCryptoSalt());

        byte[] credentialKey = randomDataGenerator.generateBytes(32);
        CryptoResult cryptoResult = cipher.encrypt(ck, credentialKey);

        u.setInitializationVectorCredentialKey(
                cryptoResult.getInitializationVector()
        );
        u.setCredentialKeyCipherText(
                cryptoResult.getCiphertext()
        );

        return u;
    }
}
