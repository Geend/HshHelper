package domainlogic.loginmanager;

import extension.HashHelper;
import extension.RecaptchaHelper;
import models.User;
import play.mvc.Http;
import policy.ext.loginFirewall.Firewall;
import policy.ext.loginFirewall.Instance;
import policy.ext.loginFirewall.Strategy;
import policy.session.SessionManager;
import sun.rmi.runtime.Log;

import javax.inject.Inject;

public class LoginManager {

    private HashHelper hashHelper;

    @Inject
    public LoginManager(HashHelper hashHelper){
        this.hashHelper = hashHelper;
    }

    private User authenticate(String username, String password, String captchaToken) throws CaptchaRequiredException, InvalidUsernameOrPasswordException {
        Authentification.Result auth = Authentification.Perform(
            username,
            password
        );

        Long uid = null;
        if(auth.userExists()) {
            uid = auth.user().getUserId();
        }

        Instance fw = Firewall.Get(Http.Context.current().request().remoteAddress());
        Strategy strategy = fw.getStrategy(uid);

        if(strategy.equals(Strategy.BLOCK)) {
            throw new InvalidUsernameOrPasswordException();
        }

        if(strategy.equals(Strategy.VERIFY)) {
            if(!RecaptchaHelper.IsValidResponse(captchaToken, Http.Context.current().request().remoteAddress())) {
                throw new CaptchaRequiredException();
            }
        }

        if(!auth.success()) {
            fw.fail(uid);
            throw new InvalidUsernameOrPasswordException();
        }

        return auth.user();
    }

    public void login(String username, String password, String captchaToken) throws CaptchaRequiredException, InvalidUsernameOrPasswordException, PasswordChangeRequiredException {
        User authenticatedUser = this.authenticate(username, password, captchaToken);

        if(authenticatedUser.getIsPasswordResetRequired()) {
            throw new PasswordChangeRequiredException();
        }

        SessionManager.StartNewSession(authenticatedUser);
    }

    public void changePassword(String username, String currentPassword, String newPassword, String captchaToken) throws InvalidUsernameOrPasswordException, CaptchaRequiredException {
        User authenticatedUser = this.authenticate(username, currentPassword, captchaToken);

        authenticatedUser.setIsPasswordResetRequired(false);
        authenticatedUser.setPasswordHash(hashHelper.hashPassword(newPassword));
        authenticatedUser.save();
    }

    public void logout() {
        SessionManager.DestroyCurrentSession();
    }
}
