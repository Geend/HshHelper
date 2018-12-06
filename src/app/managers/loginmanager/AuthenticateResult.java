package managers.loginmanager;

import models.User;

public class AuthenticateResult {
    public static AuthenticateResult Invalid(boolean captchaRequired) {
        return new AuthenticateResult( null, captchaRequired, false, false);
    }

    private User authenticatedUser;
    private boolean captchaRequired;
    private boolean authenticationSucceeded;
    private boolean passwordChangeRequired;

    AuthenticateResult(User authenticatedUser, boolean captchaRequired, boolean authenticationSucceeded, boolean passwordChangeRequired) {
        this.authenticatedUser = authenticatedUser;
        this.captchaRequired = captchaRequired;
        this.authenticationSucceeded = authenticationSucceeded;
        this.passwordChangeRequired = passwordChangeRequired;
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }

    public boolean isCaptchaRequired() {
        return captchaRequired;
    }

    public boolean isAuthenticationSucceeded() {
        return authenticationSucceeded;
    }

    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }
}
