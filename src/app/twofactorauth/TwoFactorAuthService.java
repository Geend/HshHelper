package twofactorauth;

import managers.loginmanager.InvalidLoginException;

import java.security.GeneralSecurityException;

public class TwoFactorAuthService {
    public boolean validateCurrentNumber(String base32Secret, int authNumber, int windowMillis) throws GeneralSecurityException {
        return TimeBasedOneTimePasswordUtil.validateCurrentNumber(base32Secret, authNumber, windowMillis);
    }


    public int stringPinToInt(String pin) throws InvalidLoginException {

        int intTwoFactorPin = 0;
        if(pin != null) {
            if (!pin.equals("")) {
                try {
                    String tokenWithoutWhiteSpace = pin.replaceAll(" ", "");
                    intTwoFactorPin = Integer.parseInt(tokenWithoutWhiteSpace);
                } catch (NumberFormatException e) {
                    throw new InvalidLoginException(false);
                }
            }
        }
        return intTwoFactorPin;
    }
    public String generateBase32Secret() {
        return TimeBasedOneTimePasswordUtil.generateBase32Secret();
    }
}
