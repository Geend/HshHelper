package managers.loginmanager;

public class InvalidLoginException extends Exception {
    private boolean captchaRequired;

    public InvalidLoginException(boolean captchaRequired) {
        this.captchaRequired = captchaRequired;
    }

    public boolean isCaptchaRequired() {
        return captchaRequired;
    }
}
