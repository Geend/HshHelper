package managers.loginmanager;

import extension.HashHelper;
import extension.RecaptchaHelper;
import models.LoginAttempt;
import models.User;
import models.finders.LoginAttemptFinder;
import org.joda.time.DateTime;
import play.mvc.Http;
import policyenforcement.ext.loginFirewall.Firewall;
import policyenforcement.ext.loginFirewall.Instance;
import policyenforcement.ext.loginFirewall.Strategy;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import java.util.List;

public class LoginManager {

    private Authentification authentification;
    private Firewall loginFirewall;
    private SessionManager sessionManager;
    private HashHelper hashHelper;

    @Inject
    public LoginManager(Authentification authentification, Firewall loginFirewall, SessionManager sessionManager, HashHelper hashHelper){
        this.authentification = authentification;
        this.loginFirewall = loginFirewall;
        this.sessionManager = sessionManager;
        this.hashHelper = hashHelper;
    }

    private User authenticate(String username, String password, String captchaToken) throws CaptchaRequiredException, InvalidLoginException {
        Authentification.Result auth = authentification.Perform(
            username,
            password
        );
        Http.Request request = Http.Context.current().request();
        Long uid = null;
        if(auth.userExists()) {
            uid = auth.user().getUserId();
        } else {
            // Negativen Zahlenraum für "virtuelle" UIDs (Hashmapping) nutzen
            Long fakeUid = hashHelper.insecureStringHash(username);
            if(fakeUid > 0) {
                fakeUid = fakeUid * -1;
            }
            uid = fakeUid;
        }

        Instance fw = loginFirewall.get(request.remoteAddress());
        Strategy strategy = fw.getStrategy(uid);

        if(strategy.equals(Strategy.BLOCK)) {
            throw new InvalidLoginException();
        }

        if(strategy.equals(Strategy.VERIFY)) {
            if(!RecaptchaHelper.IsValidResponse(captchaToken, Http.Context.current().request().remoteAddress())) {
                throw new CaptchaRequiredException();
            }
        }

        if(!auth.success()) {
            fw.fail(uid);
            throw new InvalidLoginException();
        }

        String userAgent = request.getHeaders().get("User-Agent").get();
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUser(auth.user());
        attempt.setAddress(request.remoteAddress());
        attempt.setClientName(userAgent);
        attempt.setDateTime(DateTime.now());
        attempt.save();

        LoginAttemptFinder finder = new LoginAttemptFinder();
        List<LoginAttempt> attempts = finder.all();
        for(int i = 5; i < attempts.size(); i++) {
            attempts.get(i).delete();
        }

        return auth.user();
    }

    public void login(String username, String password, String captchaToken) throws CaptchaRequiredException, InvalidLoginException, PasswordChangeRequiredException {
        User authenticatedUser = this.authenticate(username, password, captchaToken);

        if(authenticatedUser.getIsPasswordResetRequired()) {
            throw new PasswordChangeRequiredException();
        }

        sessionManager.startNewSession(authenticatedUser);
    }

    public void changePassword(String username, String currentPassword, String newPassword, String captchaToken) throws InvalidLoginException, CaptchaRequiredException {
        User authenticatedUser = this.authenticate(username, currentPassword, captchaToken);

        authenticatedUser.setIsPasswordResetRequired(false);
        authenticatedUser.setPasswordHash(hashHelper.hashPassword(newPassword));
        authenticatedUser.save();
    }

    public void logout() {
        sessionManager.destroyCurrentSession();
    }
}
