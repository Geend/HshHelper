package managers.loginmanager;

import extension.HashHelper;
import extension.RecaptchaHelper;
import io.ebean.EbeanServer;
import models.LoginAttempt;
import models.User;
import models.finders.LoginAttemptFinder;
import org.joda.time.DateTime;
import play.mvc.Http;
import policyenforcement.ext.loginFirewall.Firewall;
import policyenforcement.ext.loginFirewall.Instance;
import policyenforcement.ext.loginFirewall.Strategy;
import policyenforcement.session.SessionManager;
import ua_parser.Client;
import ua_parser.Parser;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class LoginManager {

    private final EbeanServer ebeanSever;
    private Authentification authentification;
    private Firewall loginFirewall;
    private SessionManager sessionManager;
    private HashHelper hashHelper;
    private LoginAttemptFinder loginAttemptFinder;

    @Inject
    public LoginManager(
            Authentification authentification,
            Firewall loginFirewall,
            SessionManager sessionManager,
            HashHelper hashHelper,
            EbeanServer ebeanServer,
            LoginAttemptFinder loginAttemptFinder)
    {
        this.loginAttemptFinder = loginAttemptFinder;
        this.ebeanSever = ebeanServer;
        this.authentification = authentification;
        this.loginFirewall = loginFirewall;
        this.sessionManager = sessionManager;
        this.hashHelper = hashHelper;
    }

    private User authenticate(String username, String password, String captchaToken, Http.Request request) throws CaptchaRequiredException, InvalidLoginException, IOException {
        Authentification.Result auth = authentification.Perform(
            username,
            password
        );
        Long uid;
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
            if(!RecaptchaHelper.IsValidResponse(captchaToken, request.remoteAddress())) {
                throw new CaptchaRequiredException();
            }
        }

        if(!auth.success()) {
            fw.fail(uid);
            throw new InvalidLoginException();
        }

        String userAgentString = request.getHeaders().get("User-Agent").get();
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUser(auth.user());
        attempt.setAddress(request.remoteAddress());
        attempt.setClientName(this.getUserAgentDisplayString(userAgentString));
        attempt.setDateTime(DateTime.now());
        this.ebeanSever.save(attempt);

        List<LoginAttempt> attempts = loginAttemptFinder.all();
        for(int i = 5; i < attempts.size(); i++) {
            this.ebeanSever.delete(attempts.get(i));
        }

        return auth.user();
    }

    private String getUserAgentDisplayString(String userAgentString) throws IOException {
        Parser uaParser = new Parser();
        Client c = uaParser.parse(userAgentString);
        return String.format("%s: %s (s)", c.device.family, c.userAgent.family, c.userAgent.major);
    }

    public void login(String username, String password, String captchaToken, Http.Request request) throws CaptchaRequiredException, InvalidLoginException, PasswordChangeRequiredException, IOException {
        User authenticatedUser = this.authenticate(username, password, captchaToken, request);

        if(authenticatedUser.getIsPasswordResetRequired()) {
            throw new PasswordChangeRequiredException();
        }

        sessionManager.startNewSession(authenticatedUser);
    }

    public void changePassword(String username, String currentPassword, String newPassword, String captchaToken, Http.Request request) throws InvalidLoginException, CaptchaRequiredException, IOException {
        User authenticatedUser = this.authenticate(username, currentPassword, captchaToken, request);

        authenticatedUser.setIsPasswordResetRequired(false);
        authenticatedUser.setPasswordHash(hashHelper.hashPassword(newPassword));
        this.ebeanSever.save(authenticatedUser);
    }

    public void logout() {
        sessionManager.destroyCurrentSession();
    }
}
