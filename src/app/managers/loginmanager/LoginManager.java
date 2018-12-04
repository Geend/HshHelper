package managers.loginmanager;

import extension.CredentialManager;
import extension.HashHelper;
import extension.RecaptchaHelper;
import extension.WeakPasswords;
import extension.logging.DangerousCharFilteringLogger;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.WeakPasswordException;
import models.LoginAttempt;
import models.PasswordResetToken;
import models.User;
import models.finders.LoginAttemptFinder;
import models.finders.PasswordResetTokenFinder;
import models.finders.UserFinder;
import org.joda.time.DateTime;
import play.Logger;
import play.libs.mailer.Email;
import play.libs.mailer.MailerClient;
import play.mvc.Http;
import policyenforcement.ext.loginFirewall.Firewall;
import policyenforcement.ext.loginFirewall.Instance;
import policyenforcement.ext.loginFirewall.Strategy;
import policyenforcement.session.SessionManager;
import ua_parser.Client;
import ua_parser.Parser;

import javax.inject.Inject;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.UUID;

import static policyenforcement.ConstraintValues.PASSWORD_RESET_TOKEN_TIMEOUT_HOURS;
import static policyenforcement.ConstraintValues.SUCCESSFUL_LOGIN_STORAGE_DURATION_DAYS;

public class LoginManager {

    private final UserFinder userFinder;
    private final EbeanServer ebeanSever;
    private Authentification authentification;
    private Firewall loginFirewall;
    private SessionManager sessionManager;
    private HashHelper hashHelper;
    private LoginAttemptFinder loginAttemptFinder;
    private final RecaptchaHelper recaptchaHelper;
    private final CredentialManager credentialManager;
    private final MailerClient mailerClient;
    private final PasswordResetTokenFinder passwordResetTokenFinder;
    private final WeakPasswords weakPasswords;

    private static final Logger.ALogger logger = new DangerousCharFilteringLogger(LoginManager.class);

    @Inject
    public LoginManager(
            Authentification authentification,
            Firewall loginFirewall,
            SessionManager sessionManager,
            HashHelper hashHelper,
            EbeanServer ebeanServer,
            LoginAttemptFinder loginAttemptFinder,
            RecaptchaHelper recaptchaHelper, CredentialManager credentialManager,
            UserFinder userFinder, MailerClient mailerClient, PasswordResetTokenFinder passwordResetTokenFinder, WeakPasswords weakPasswords)
    {
        this.loginAttemptFinder = loginAttemptFinder;
        this.ebeanSever = ebeanServer;
        this.authentification = authentification;
        this.loginFirewall = loginFirewall;
        this.sessionManager = sessionManager;
        this.hashHelper = hashHelper;
        this.recaptchaHelper = recaptchaHelper;
        this.credentialManager = credentialManager;
        this.userFinder = userFinder;
        this.mailerClient = mailerClient;
        this.passwordResetTokenFinder = passwordResetTokenFinder;
        this.weakPasswords = weakPasswords;
    }

    private User authenticate(String username, String password, String captchaToken, Http.Request request, Integer twoFactorPin) throws CaptchaRequiredException, InvalidLoginException, IOException, GeneralSecurityException {
        Authentification.Result auth = authentification.Perform(username, password, twoFactorPin);
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
            logger.error(request.remoteAddress() + " is blocked from logging in.");
            throw new InvalidLoginException();
        }

        if(strategy.equals(Strategy.VERIFY)) {
            if(!recaptchaHelper.IsValidResponse(captchaToken, request.remoteAddress())) {
                logger.error(request.remoteAddress() + " has tried to login in without a valid reCAPTCHA.");
                throw new CaptchaRequiredException();
            }
        }

        if(!auth.success()) {
            fw.fail(uid);
            logger.error(request.remoteAddress() + " failed to login on user " + uid);
            throw new InvalidLoginException();
        }


        Optional<String> userAgentString = request.getHeaders().get("User-Agent");
        if(!userAgentString.isPresent()) {
            throw new InvalidLoginException();
        }

        LoginAttempt attempt = new LoginAttempt();
        attempt.setUser(auth.user());
        attempt.setAddress(request.remoteAddress());
        attempt.setClientName(this.getUserAgentDisplayString(userAgentString.get()));
        attempt.setDateTime(DateTime.now());
        this.ebeanSever.save(attempt);

        return auth.user();
    }

    private String getUserAgentDisplayString(String userAgentString) throws IOException {
        Parser uaParser = new Parser();
        Client c = uaParser.parse(userAgentString);
        return String.format("%s: %s (%s)", c.device.family, c.userAgent.family, c.userAgent.major);
    }

    public void login(String username, String password, String captchaToken, Http.Request request, Integer twoFactorPin) throws CaptchaRequiredException, InvalidLoginException, PasswordChangeRequiredException, IOException, GeneralSecurityException {
        User authenticatedUser = this.authenticate(username, password, captchaToken, request, twoFactorPin);

        if(authenticatedUser.getIsPasswordResetRequired()) {
            logger.error(authenticatedUser + " needs to change his password.");
            throw new PasswordChangeRequiredException();
        }

        byte[] credentialKeyPlaintext = credentialManager.getCredentialPlaintext(authenticatedUser, password);

        sessionManager.startNewSession(authenticatedUser, credentialKeyPlaintext);
        logger.info(authenticatedUser + " has logged in.");
    }

    public void changePassword(String username, String currentPassword, String newPassword, String captchaToken, Http.Request request, Integer twoFactorPin) throws InvalidLoginException, CaptchaRequiredException, IOException, GeneralSecurityException, WeakPasswordException {
        User authenticatedUser = this.authenticate(username, currentPassword, captchaToken, request, twoFactorPin);

        if(weakPasswords.isWeakPw(newPassword)) {
            throw new WeakPasswordException();
        }

        try(Transaction tx = ebeanSever.beginTransaction(TxIsolation.SERIALIZABLE)) {
            credentialManager.updateCredentialPassword(currentPassword, newPassword);
            authenticatedUser.setIsPasswordResetRequired(false);
            authenticatedUser.setPasswordHash(hashHelper.hashPassword(newPassword));
            ebeanSever.save(authenticatedUser);
            tx.commit();
        }

        logger.info(authenticatedUser + " changed his password.");
    }

    public void logout() {
        sessionManager.destroyCurrentSession();
    }

    public void deleteOldLoginRecords() {
        int deletedSessions = loginAttemptFinder.query().where()
                .lt("dateTime",DateTime.now().minusDays(SUCCESSFUL_LOGIN_STORAGE_DURATION_DAYS)).delete();

        logger.info("Deleted "+deletedSessions+" Login-Logs");
    }

    public void sendResetPasswordToken(String username, String recaptcha, Http.Request request) throws CaptchaRequiredException, InvalidArgumentException {
        if(!recaptchaHelper.IsValidResponse(recaptcha, request.remoteAddress())) {
            // TODO: encode username -> log injection
            logger.error(request.remoteAddress() + " has tried to reset the password for user " + username + " without a valid reCAPTCHA.");
            throw new CaptchaRequiredException();
        }

        Optional<User> userOptional = userFinder.byName(username);
        if (!userOptional.isPresent()) {
            throw new InvalidArgumentException("Dieser User existiert nicht.");
        }
        User user = userOptional.get();

        PasswordResetToken token = new PasswordResetToken(
            user,
            request.remoteAddress()
        );
        ebeanSever.save(token);

        Email email = new Email()
                .setSubject("HshHelper Password-Reset")
                .setFrom("HshHelper <hshhelper@t-voltmer.net>")
                .addTo(user.getEmail())
                .setBodyText("Click on the following Link to reset your Password: "+ controllers.routes.LoginController.showResetPasswordWithTokenForm(token.getId()).absoluteURL(request));
        mailerClient.send(email);

        logger.info("Created a reset token and send mail for user " + user);
    }

    public PasswordResetToken validateResetToken(UUID tokenId, Http.Request request) throws UnauthorizedException {
        PasswordResetToken token = passwordResetTokenFinder.byId(tokenId);
        if(token == null)
            throw new UnauthorizedException();

        if(!token.getRemoteAddress().equals(request.remoteAddress()))
            throw new UnauthorizedException();

        if(!token.getCreationDate().plusHours(PASSWORD_RESET_TOKEN_TIMEOUT_HOURS).isAfterNow())
            throw new UnauthorizedException();

        return token;
    }

    public void resetPassword(UUID tokenId, String newPassword, Http.Request request) throws UnauthorizedException, WeakPasswordException {
        if(weakPasswords.isWeakPw(newPassword)) {
            throw new WeakPasswordException();
        }

        try(Transaction tx = ebeanSever.beginTransaction(TxIsolation.SERIALIZABLE)) {
            PasswordResetToken token = validateResetToken(tokenId, request);

            User user = token.getAssociatedUser();
            ebeanSever.refresh(user);
            user.setPasswordHash(hashHelper.hashPassword(newPassword));
            ebeanSever.save(user);

            credentialManager.resetCredential(user, newPassword);

            ebeanSever.delete(token);

            tx.commit();
        }

    }
}
