package managers.loginmanager;

import extension.HashHelper;
import models.User;
import models.finders.UserFinder;
import twofactorauth.TimeBasedOneTimePasswordUtil;

import javax.inject.Inject;
import java.security.GeneralSecurityException;
import java.util.Optional;

import static extension.StringHelper.empty;

public class Authentification {
    public static class Result {
        private boolean success;
        private boolean userExists;
        private User user;

        public Result(boolean success, boolean userExists, User user) {
            this.success = success;
            this.userExists = userExists;
            this.user = user;
        }

        public boolean userExists() {
            return this.userExists;
        }
        public boolean success() {
            return this.success;
        }
        public User user() {
            return this.user;
        }
    }

    private UserFinder userFinder;
    private HashHelper hashHelper;

    @Inject
    public Authentification(UserFinder userFinder, HashHelper hashHelper) {
        this.userFinder = userFinder;
        this.hashHelper = hashHelper;
    }

    public Result Perform(String username, String password, Integer twoFactorPin) throws GeneralSecurityException {
        Optional<User> user = userFinder.byName(username);

        // Nutzer existiert nicht!
        if(!user.isPresent()) {
            // Statistik-Angriff verhindern.
            // Aufrufzeit darf sich nicht maßgeblich unterscheiden, wenn kein Nutzer existiert.
            hashHelper.hashPassword(password);
            verifySecondFactor("ORT4CT7FHMPJB6X2", 250890);
            return new Result(false, false, null);
        }

        // Nutzer existiert
        boolean success = hashHelper.checkHash(password, user.get().getPasswordHash());
        String twoFactorSecret = user.get().getTwoFactorAuthSecret();
        if(success && !empty(twoFactorSecret)) {
            success = verifySecondFactor(twoFactorSecret, twoFactorPin);
        }
        return new Result(success, true, user.get());
    }

    private Boolean verifySecondFactor(String secret, Integer suppliedSecondFactor) throws GeneralSecurityException {
        // 60 sekunden zeitfenster
        return TimeBasedOneTimePasswordUtil.validateCurrentNumber(secret, suppliedSecondFactor, 60000);
    }
}
