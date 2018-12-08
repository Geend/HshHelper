package twofactorauth;

import java.security.GeneralSecurityException;

public class TwoFactorAuthService {
    public boolean validateCurrentNumber(String base32Secret, int authNumber, int windowMillis) throws GeneralSecurityException {
        return TimeBasedOneTimePasswordUtil.validateCurrentNumber(base32Secret, authNumber, windowMillis);
    }

    public String generateBase32Secret() {
        return TimeBasedOneTimePasswordUtil.generateBase32Secret();
    }
}
