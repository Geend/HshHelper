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

public class Authentification {
    private UserFinder userFinder;
    private HashHelper hashHelper;

    @Inject
    public Authentification(UserFinder userFinder, HashHelper hashHelper) {
        this.userFinder = userFinder;
        this.hashHelper = hashHelper;
    }

    public boolean perform(User user, String password, Integer twoFactorPin) throws GeneralSecurityException {
        // Nutzer existiert
        boolean success = hashHelper.checkHash(password, user.getPasswordHash());
        String twoFactorSecret = user.getTwoFactorAuthSecret();
        if(success && user.has2FA()) {
            success = verifySecondFactor(twoFactorSecret, twoFactorPin);
        }
        return success;
    }

    // Statistik-Angriff verhindern.
    // Aufrufzeit darf sich nicht maßgeblich unterscheiden, wenn kein Nutzer existiert.
    public void fakeAuthActionsForTiming(String password, Integer twoFactorPin) {
        hashHelper.hashPassword(password);
        try {
            verifySecondFactor("ORT4CT7FHMPJB6X2", twoFactorPin);
        } catch (GeneralSecurityException e) {}
    }

    private Boolean verifySecondFactor(String secret, Integer suppliedSecondFactor) throws GeneralSecurityException {
        // 60 sekunden zeitfenster
        return TimeBasedOneTimePasswordUtil.validateCurrentNumber(secret, suppliedSecondFactor, TIME_WINDOW_2FA_MS);
    }
}
