package managers.loginmanager;

import extension.HashHelper;
import models.User;
import models.finders.UserFinder;
import twofactorauth.TimeBasedOneTimePasswordUtil;

import javax.inject.Inject;
import java.security.GeneralSecurityException;
import java.util.Optional;

import static extension.StringHelper.empty;
import static policyenforcement.ConstraintValues.TIME_WINDOW_2FA_MS;

public class UserAuth {
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
    public UserAuth(UserFinder userFinder, HashHelper hashHelper) {
        this.userFinder = userFinder;
        this.hashHelper = hashHelper;
    }

    public Result Perform(String username, String password, Integer twoFactorPin) throws GeneralSecurityException {
        Optional<User> userOpt = userFinder.byName(username);

        // Nutzer existiert nicht!
        if(!userOpt.isPresent()) {
            // Statistik-Angriff verhindern.
            // Aufrufzeit darf sich nicht maßgeblich unterscheiden, wenn kein Nutzer existiert.
            hashHelper.hashPassword(password);
            verifySecondFactor("ORT4CT7FHMPJB6X2", 250890);
            return new Result(false, false, null);
        }

        User user = userOpt.get();

        // Nutzer existiert
        boolean success = hashHelper.checkHash(password, user.getPasswordHash());
        String twoFactorSecret = user.getTwoFactorAuthSecret();
        if(success && user.has2FA()) {
            success = verifySecondFactor(twoFactorSecret, twoFactorPin);
        }
        return new Result(success, true, user);
    }

    private Boolean verifySecondFactor(String secret, Integer suppliedSecondFactor) throws GeneralSecurityException {
        // 60 sekunden zeitfenster
        return TimeBasedOneTimePasswordUtil.validateCurrentNumber(secret, suppliedSecondFactor, TIME_WINDOW_2FA_MS);
    }
}
