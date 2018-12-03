package managers.loginmanager;

import extension.CredentialManager;
import extension.CredentialSerializer.Credential;
import extension.Crypto.Cipher;
import extension.Crypto.KeyGenerator;
import extension.HashHelper;
import extension.RecaptchaHelper;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import models.PasswordResetToken;
import models.User;
import models.finders.LoginAttemptFinder;
import models.finders.PasswordResetTokenFinder;
import models.finders.UserFinder;
import org.joda.time.DateTime;
import org.junit.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import play.libs.mailer.MailerClient;
import play.mvc.Http;
import play.test.Helpers;
import policyenforcement.ext.loginFirewall.Firewall;
import policyenforcement.ext.loginFirewall.Instance;
import policyenforcement.ext.loginFirewall.Strategy;
import policyenforcement.session.SessionManager;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static policyenforcement.ConstraintValues.PASSWORD_RESET_TOKEN_TIMEOUT_HOURS;

public class LoginManagerTest {
    private HashHelper defaultHashHelper;
    private UserFinder defaultUserFinder;
    private Firewall defaultFirewall;
    private SessionManager defaultSessionManager;
    private Authentification defaultAuthentification;
    private EbeanServer defaultEbeanServer;
    private Instance defaultFirewallInstance;
    private LoginAttemptFinder defaultLoginAttemptFinder;
    private Http.Headers defaultHeaders;
    private Http.Request defaultRequest;
    private RecaptchaHelper recaptchaHelper;
    private CredentialManager defaultCredentialManager;
    private byte[] defaultCredentialKey;
    private MailerClient defaultMailerClient;
    private PasswordResetTokenFinder defaultPasswordResetTokenFinder;
    private LoginManager defaultLoginManager;


    @Before
    public void init() {
        this.defaultHeaders = mock(Http.Headers.class);
        this.defaultRequest = mock(Http.Request.class);
        when(defaultRequest.remoteAddress()).thenReturn("127.0.0.1");
        when(defaultRequest.getHeaders()).thenReturn(this.defaultHeaders);
        when(this.defaultHeaders.get("User-Agent")).thenReturn(Optional.of("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:63.0) Gecko/20100101 Firefox/63.0"));
        this.defaultLoginAttemptFinder = mock(LoginAttemptFinder.class);
        this.defaultUserFinder = mock(UserFinder.class);
        this.defaultFirewall = mock(Firewall.class);
        this.defaultSessionManager = mock(SessionManager.class);
        this.defaultAuthentification = mock(Authentification.class);
        this.defaultHashHelper = mock(HashHelper.class);
        this.defaultEbeanServer = mock(EbeanServer.class);
        this.defaultFirewallInstance = mock(Instance.class);
        this.recaptchaHelper = mock(RecaptchaHelper.class);
        when(this.defaultFirewallInstance.getStrategy(any(Long.class))).thenReturn(Strategy.BYPASS);
        when(defaultFirewall.get(any(String.class))).thenReturn(this.defaultFirewallInstance);

        defaultCredentialKey = new byte[]{1,2,3,4};
        defaultCredentialManager = mock(CredentialManager.class);
        when(defaultCredentialManager.getCredentialPlaintext(any(), any())).thenReturn(defaultCredentialKey);
        when(defaultCredentialManager.getCredentialPlaintext(any())).thenReturn(defaultCredentialKey);

        this.defaultMailerClient = mock(MailerClient.class);
        this.defaultPasswordResetTokenFinder = mock(PasswordResetTokenFinder.class);

        this.defaultLoginManager = new LoginManager(
                defaultAuthentification,
                defaultFirewall,
                this.defaultSessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder,
                this.recaptchaHelper, defaultCredentialManager,
                defaultUserFinder, defaultMailerClient, defaultPasswordResetTokenFinder
        );
    }

    @Test
    public void successfulLogin() throws InvalidLoginException, PasswordChangeRequiredException, CaptchaRequiredException, IOException, GeneralSecurityException {
        User authenticatedUser = mock(User.class);
        when(authenticatedUser.getUserId()).thenReturn(5l);
        Authentification auth = mock(Authentification.class);
        SessionManager sessionManager = mock(SessionManager.class);
        when(auth.Perform(any(String.class), any(String.class), any(Integer.class))).thenReturn(new Authentification.Result(true, true, authenticatedUser));
        LoginManager sut = new LoginManager(
                auth,
                this.defaultFirewall,
                sessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder,
                this.recaptchaHelper, defaultCredentialManager,
                defaultUserFinder, defaultMailerClient, defaultPasswordResetTokenFinder
        );
        sut.login("lydia", "lydia", "", this.defaultRequest, 0);
        verify(sessionManager).startNewSession(authenticatedUser, defaultCredentialKey);
    }

    @Test(expected = PasswordChangeRequiredException.class)
    public void successfulLoginResetRequired() throws InvalidLoginException, PasswordChangeRequiredException, CaptchaRequiredException, IOException, GeneralSecurityException {
        User authenticatedUser = mock(User.class);
        when(authenticatedUser.getUserId()).thenReturn(5l);
        when(authenticatedUser.getIsPasswordResetRequired()).thenReturn(true);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class), any(Integer.class))).thenReturn(new Authentification.Result(true, true, authenticatedUser));
        LoginManager sut = new LoginManager(
                auth,
                this.defaultFirewall,
                this.defaultSessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder,
                this.recaptchaHelper, defaultCredentialManager,
                defaultUserFinder, defaultMailerClient, defaultPasswordResetTokenFinder
        );
        sut.login("lydia", "lydia", "", this.defaultRequest, 0);
    }

    @Test(expected = InvalidLoginException.class)
    public void failedLogin() throws InvalidLoginException, PasswordChangeRequiredException, CaptchaRequiredException, IOException, GeneralSecurityException {
        User authenticatedUser = mock(User.class);
        when(authenticatedUser.getUserId()).thenReturn(5l);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class), any(Integer.class))).thenReturn(new Authentification.Result(false, true, authenticatedUser));
        LoginManager sut = new LoginManager(
                auth,
                this.defaultFirewall,
                this.defaultSessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder,
                this.recaptchaHelper, defaultCredentialManager,
                defaultUserFinder, defaultMailerClient, defaultPasswordResetTokenFinder
        );
        sut.login("lydia", "lydia", "", this.defaultRequest, 0);
    }


    @Test
    public void successfulChangePassword() throws InvalidLoginException, CaptchaRequiredException, IOException, GeneralSecurityException {
        User authenticatedUser = mock(User.class);
        when(authenticatedUser.getUserId()).thenReturn(5l);
        when(authenticatedUser.getIsPasswordResetRequired()).thenReturn(true);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class), any(Integer.class))).thenReturn(new Authentification.Result(true, true, authenticatedUser));
        HashHelper hashHelper = mock(HashHelper.class);
        when(hashHelper.hashPassword(any(String.class))).thenReturn("hashed");
        EbeanServer s = mock(EbeanServer.class);
        LoginManager sut = new LoginManager(
                auth,
                this.defaultFirewall,
                this.defaultSessionManager,
                hashHelper,
                s,
                this.defaultLoginAttemptFinder,
                this.recaptchaHelper, defaultCredentialManager,
                defaultUserFinder, defaultMailerClient, defaultPasswordResetTokenFinder
        );
        sut.changePassword("lydia", "lydia", "neuespw", "", this.defaultRequest, 0);
        verify(authenticatedUser).setIsPasswordResetRequired(false);
        verify(authenticatedUser).setPasswordHash("hashed");
        verify(s).save(authenticatedUser);
    }

    @Test(expected = InvalidLoginException.class)
    public void failedChangePassword() throws InvalidLoginException, CaptchaRequiredException, IOException, GeneralSecurityException {
        User authenticatedUser = mock(User.class);
        when(authenticatedUser.getUserId()).thenReturn(5l);
        when(authenticatedUser.getIsPasswordResetRequired()).thenReturn(true);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class), any(Integer.class))).thenReturn(new Authentification.Result(false, true, authenticatedUser));
        LoginManager sut = new LoginManager(
                auth,
                this.defaultFirewall,
                this.defaultSessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder,
                this.recaptchaHelper, defaultCredentialManager,
                defaultUserFinder, defaultMailerClient, defaultPasswordResetTokenFinder
        );
        sut.changePassword("lydia", "lydia", "neuespw", "", this.defaultRequest, 0);
    }

    @Test(expected = CaptchaRequiredException.class)
    public void validLoginCaptchaRequired() throws InvalidLoginException, CaptchaRequiredException, PasswordChangeRequiredException, IOException, GeneralSecurityException {
        User authenticatedUser = mock(User.class);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class), any(Integer.class))).thenReturn(new Authentification.Result(true, true, authenticatedUser));
        Instance i = mock(Instance.class);
        Firewall fw = mock(Firewall.class);
        when(fw.get(any(String.class))).thenReturn(i);
        when(i.getStrategy(any(Long.class))).thenReturn(Strategy.VERIFY);
        LoginManager sut = new LoginManager(
                auth,
                fw,
                this.defaultSessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder,
                this.recaptchaHelper, defaultCredentialManager,
                defaultUserFinder, defaultMailerClient, defaultPasswordResetTokenFinder
        );
        sut.login("lydia", "lydia", "", this.defaultRequest, 0);
    }


    @Test(expected = CaptchaRequiredException.class)
    public void invalidLoginCaptchaRequired() throws InvalidLoginException, CaptchaRequiredException, PasswordChangeRequiredException, IOException, GeneralSecurityException {
        User authenticatedUser = mock(User.class);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class), any(Integer.class))).thenReturn(new Authentification.Result(false, true, authenticatedUser));
        Instance i = mock(Instance.class);
        Firewall fw = mock(Firewall.class);
        when(fw.get(any(String.class))).thenReturn(i);
        when(i.getStrategy(any(Long.class))).thenReturn(Strategy.VERIFY);
        LoginManager sut = new LoginManager(
                auth,
                fw,
                this.defaultSessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder,
                this.recaptchaHelper, defaultCredentialManager,
                defaultUserFinder, defaultMailerClient, defaultPasswordResetTokenFinder
        );
        sut.login("lydia", "lydia", "", this.defaultRequest, 0);
    }


    @Test(expected = InvalidLoginException.class)
    public void validLoginBanned() throws InvalidLoginException, CaptchaRequiredException, PasswordChangeRequiredException, IOException, GeneralSecurityException {
        User authenticatedUser = mock(User.class);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class), any(Integer.class))).thenReturn(new Authentification.Result(false, true, authenticatedUser));
        Instance i = mock(Instance.class);
        Firewall fw = mock(Firewall.class);
        when(fw.get(any(String.class))).thenReturn(i);
        when(i.getStrategy(any(Long.class))).thenReturn(Strategy.BLOCK);
        LoginManager sut = new LoginManager(
                auth,
                fw,
                this.defaultSessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder,
                this.recaptchaHelper, defaultCredentialManager,
                defaultUserFinder, defaultMailerClient, defaultPasswordResetTokenFinder
        );
        sut.login("lydia", "lydia", "", this.defaultRequest, 0);
    }

    @Test(expected = InvalidLoginException.class)
    public void invalidLoginBannedWithCorrectLogin() throws InvalidLoginException, CaptchaRequiredException, PasswordChangeRequiredException, IOException, GeneralSecurityException {
        User authenticatedUser = mock(User.class);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class), any(Integer.class))).thenReturn(new Authentification.Result(true, true, authenticatedUser));
        Instance i = mock(Instance.class);
        Firewall fw = mock(Firewall.class);
        when(fw.get(any(String.class))).thenReturn(i);
        when(i.getStrategy(any(Long.class))).thenReturn(Strategy.BLOCK);
        LoginManager sut = new LoginManager(
                auth,
                fw,
                this.defaultSessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder,
                this.recaptchaHelper, defaultCredentialManager,
                defaultUserFinder, defaultMailerClient, defaultPasswordResetTokenFinder
        );
        sut.login("lydia", "lydia", "", this.defaultRequest, 0);
    }


    @Test(expected = CaptchaRequiredException.class)
    public void validPasswordChangeCaptchaRequired() throws InvalidLoginException, CaptchaRequiredException, IOException, GeneralSecurityException {
        User authenticatedUser = mock(User.class);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class), any(Integer.class))).thenReturn(new Authentification.Result(true, true, authenticatedUser));
        Instance i = mock(Instance.class);
        Firewall fw = mock(Firewall.class);
        when(fw.get(any(String.class))).thenReturn(i);
        when(i.getStrategy(any(Long.class))).thenReturn(Strategy.VERIFY);
        LoginManager sut = new LoginManager(
                auth,
                fw,
                this.defaultSessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder,
                this.recaptchaHelper, defaultCredentialManager,
                defaultUserFinder, defaultMailerClient, defaultPasswordResetTokenFinder
        );
        sut.changePassword("lydia", "lydia", "neuespw", "", this.defaultRequest, 0);
    }


    @Test(expected = CaptchaRequiredException.class)
    public void invalidPasswordChangeCaptchaRequired() throws InvalidLoginException, CaptchaRequiredException, IOException, GeneralSecurityException {
        User authenticatedUser = mock(User.class);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class), any(Integer.class))).thenReturn(new Authentification.Result(false, true, authenticatedUser));
        Instance i = mock(Instance.class);
        Firewall fw = mock(Firewall.class);
        when(fw.get(any(String.class))).thenReturn(i);
        when(i.getStrategy(any(Long.class))).thenReturn(Strategy.VERIFY);
        LoginManager sut = new LoginManager(
                auth,
                fw,
                this.defaultSessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder,
                this.recaptchaHelper, defaultCredentialManager,
                defaultUserFinder, defaultMailerClient, defaultPasswordResetTokenFinder
        );
        sut.changePassword("lydia", "lydia", "neuespw", "", this.defaultRequest, 0);
    }


    @Test(expected = InvalidLoginException.class)
    public void validPasswordChangeBanned() throws InvalidLoginException, CaptchaRequiredException, IOException, GeneralSecurityException {
        User authenticatedUser = mock(User.class);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class), any(Integer.class))).thenReturn(new Authentification.Result(true, true, authenticatedUser));
        Instance i = mock(Instance.class);
        Firewall fw = mock(Firewall.class);
        when(fw.get(any(String.class))).thenReturn(i);
        when(i.getStrategy(any(Long.class))).thenReturn(Strategy.BLOCK);
        LoginManager sut = new LoginManager(
                auth,
                fw,
                this.defaultSessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder,
                this.recaptchaHelper, defaultCredentialManager,
                defaultUserFinder, defaultMailerClient, defaultPasswordResetTokenFinder
        );
        sut.changePassword("lydia", "lydia", "neuespw", "", this.defaultRequest, 0);
    }


    @Test(expected = InvalidLoginException.class)
    public void invalidPasswordChangeBanned() throws InvalidLoginException, CaptchaRequiredException, IOException, GeneralSecurityException {
        User authenticatedUser = mock(User.class);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class), any(Integer.class))).thenReturn(new Authentification.Result(false, true, authenticatedUser));
        Instance i = mock(Instance.class);
        Firewall fw = mock(Firewall.class);
        when(fw.get(any(String.class))).thenReturn(i);
        when(i.getStrategy(any(Long.class))).thenReturn(Strategy.BLOCK);
        LoginManager sut = new LoginManager(
                auth,
                fw,
                this.defaultSessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder,
                this.recaptchaHelper, defaultCredentialManager,
                defaultUserFinder, defaultMailerClient, defaultPasswordResetTokenFinder
        );
        sut.changePassword("lydia", "lydia", "neuespw", "", this.defaultRequest, 0);
    }

    @Test
    public void validateTokenValidTokenTest() throws UnauthorizedException {
        UUID tokenId = UUID.fromString("0bce3cf6-e7d7-422c-a2ad-0a080a2d4b68");

        String remoteAddr = "12.2.21.2";
        PasswordResetToken token = mock(PasswordResetToken.class);
        when(token.getRemoteAddress()).thenReturn(remoteAddr);
        when(token.getCreationDate()).thenReturn(DateTime.now());
        when(defaultPasswordResetTokenFinder.byId(tokenId)).thenReturn(token);

        defaultLoginManager.validateResetToken(tokenId, Helpers.fakeRequest().remoteAddress(remoteAddr).build());
    }

    @Test(expected = UnauthorizedException.class)
    public void validateTokenIpChangedTokenTest() throws UnauthorizedException {
        UUID tokenId = UUID.fromString("0bce3cf6-e7d7-422c-a2ad-0a080a2d4b68");

        String remoteAddr = "12.2.21.2";
        PasswordResetToken token = mock(PasswordResetToken.class);
        when(token.getRemoteAddress()).thenReturn(remoteAddr);
        when(token.getCreationDate()).thenReturn(DateTime.now());
        when(defaultPasswordResetTokenFinder.byId(tokenId)).thenReturn(token);

        defaultLoginManager.validateResetToken(tokenId, Helpers.fakeRequest().remoteAddress("1"+remoteAddr).build());
    }

    @Test(expected = UnauthorizedException.class)
    public void validateTokenTimedOutTokenTest() throws UnauthorizedException {
        UUID tokenId = UUID.fromString("0bce3cf6-e7d7-422c-a2ad-0a080a2d4b68");

        String remoteAddr = "12.2.21.2";
        PasswordResetToken token = mock(PasswordResetToken.class);
        when(token.getRemoteAddress()).thenReturn(remoteAddr);
        when(token.getCreationDate()).thenReturn(DateTime.now().minusHours(PASSWORD_RESET_TOKEN_TIMEOUT_HOURS).minusSeconds(1));
        when(defaultPasswordResetTokenFinder.byId(tokenId)).thenReturn(token);

        defaultLoginManager.validateResetToken(tokenId, Helpers.fakeRequest().remoteAddress(remoteAddr).build());
    }

    @Test(expected = UnauthorizedException.class)
    public void validateTokenNotFoundTokenTest() throws UnauthorizedException {
        UUID tokenId = UUID.fromString("0bce3cf6-e7d7-422c-a2ad-0a080a2d4b68");

        when(defaultPasswordResetTokenFinder.byId(tokenId)).thenReturn(null);

        defaultLoginManager.validateResetToken(tokenId, Helpers.fakeRequest().build());
    }

    @Test(expected = UnauthorizedException.class)
    public void resetPasswordVerificationTest() throws UnauthorizedException {
        when(defaultPasswordResetTokenFinder.byId(any())).thenReturn(null);
        defaultLoginManager.resetPassword(UUID.randomUUID(), "123", Helpers.fakeRequest().build());
    }

    @Test
    public void resetPasswordTest() throws UnauthorizedException {
        UUID id = UUID.randomUUID();
        String remoteAddr = "12.2.2.1";
        String newPassword = "123";
        String newPasswordHash = "123_HASH";

        when(defaultHashHelper.hashPassword(newPassword)).thenReturn(newPasswordHash);
        when(defaultEbeanServer.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));
        User user = mock(User.class);
        PasswordResetToken token = mock(PasswordResetToken.class);
        when(token.getCreationDate()).thenReturn(DateTime.now());
        when(token.getRemoteAddress()).thenReturn(remoteAddr);
        when(token.getAssociatedUser()).thenReturn(user);
        when(defaultPasswordResetTokenFinder.byId(id)).thenReturn(token);

        defaultLoginManager.resetPassword(id, newPassword, Helpers.fakeRequest().remoteAddress(remoteAddr).build());

        verify(user).setPasswordHash(newPasswordHash);
        verify(defaultCredentialManager).resetCredential(user, newPassword);
        verify(defaultEbeanServer).save(user);
        verify(defaultEbeanServer).delete(token);
    }

    @Test(expected = CaptchaRequiredException.class)
    public void sendResetPasswordTokenCaptchaRequiredTest() throws CaptchaRequiredException, InvalidArgumentException {
        when(recaptchaHelper.IsValidResponse(any(), any())).thenReturn(false);
        defaultLoginManager.sendResetPasswordToken("123", "231", Helpers.fakeRequest().build());
    }

    @Test(expected = InvalidArgumentException.class)
    public void sendResetPasswordTokenInvalidUserTest() throws CaptchaRequiredException, InvalidArgumentException {
        when(recaptchaHelper.IsValidResponse(any(), any())).thenReturn(true);
        when(defaultUserFinder.byName("123")).thenReturn(Optional.empty());
        defaultLoginManager.sendResetPasswordToken("123", "231", Helpers.fakeRequest().build());
    }

    @Test
    public void sendResetPasswordTokenTest() throws CaptchaRequiredException, InvalidArgumentException {
        UUID tokenId = UUID.randomUUID();
        String username = "xkac";
        User user = mock(User.class);

        when(recaptchaHelper.IsValidResponse(any(), any())).thenReturn(true);
        when(defaultUserFinder.byName(username)).thenReturn(Optional.of(user));

        doAnswer((Answer<PasswordResetToken>) invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId(tokenId);
            return token;
        }).when(defaultEbeanServer).save(any());

        defaultLoginManager.sendResetPasswordToken(username, "231", Helpers.fakeRequest().build());

        verify(defaultMailerClient).send(any());
        verify(defaultEbeanServer).save(any(PasswordResetToken.class));
    }
}