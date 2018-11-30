package extension;

import extension.Crypto.Cipher;
import extension.Crypto.CryptoKey;
import extension.Crypto.CryptoResult;
import extension.Crypto.KeyGenerator;
import models.User;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;

public class CredentialManager {
    private final SessionManager sessionManager;
    private final KeyGenerator keyGenerator;
    private final Cipher cipher;

    @Inject
    public CredentialManager(SessionManager sessionManager, KeyGenerator keyGenerator, Cipher cipher) {
        this.sessionManager = sessionManager;
        this.keyGenerator = keyGenerator;
        this.cipher = cipher;
    }

    public byte[] getCredentialPlaintext(String password) {
        User currentUser = sessionManager.currentUser();
        return getCredentialPlaintext(currentUser, password);
    }

    public byte[] getCredentialPlaintext(User user, String password) {
        CryptoKey key = keyGenerator.generate(password, user.getCryptoSalt());
        return cipher.decrypt(key, user.getInitializationVectorCredentialKey(), user.getCredentialKeyCipherText());
    }

    public void updateCredentialPassword(String oldPassword, String newPassword) {
        byte[] currentCredentialPlaintext = getCredentialPlaintext(oldPassword);

        byte[] salt = keyGenerator.generateSalt();
        CryptoKey key = keyGenerator.generate(newPassword, salt);
        CryptoResult result = cipher.encrypt(key, currentCredentialPlaintext);

        User currentUser = sessionManager.currentUser();
        currentUser.setCryptoSalt(salt);
        currentUser.setInitializationVectorCredentialKey(result.getInitializationVector());
        currentUser.setCredentialKeyCipherText(result.getCiphertext());
        currentUser.save();
    }
}
