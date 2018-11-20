package managers.loginmanager;

import extension.HashHelper;
import io.ebean.EbeanServer;
import models.User;
import models.finders.LoginAttemptFinder;
import models.finders.UserFinder;
import org.junit.*;
import play.mvc.Http;
import policyenforcement.ext.loginFirewall.Firewall;
import policyenforcement.ext.loginFirewall.Instance;
import policyenforcement.ext.loginFirewall.Strategy;
import policyenforcement.session.SessionManager;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        when(this.defaultFirewallInstance.getStrategy(any(Long.class))).thenReturn(Strategy.BYPASS);
        when(defaultFirewall.get(any(String.class))).thenReturn(this.defaultFirewallInstance);
    }

    @Test
    public void successfulLogin() throws InvalidLoginException, PasswordChangeRequiredException, CaptchaRequiredException, IOException {
        User authenticatedUser = mock(User.class);
        when(authenticatedUser.getUserId()).thenReturn(5l);
        Authentification auth = mock(Authentification.class);
        SessionManager sessionManager = mock(SessionManager.class);
        when(auth.Perform(any(String.class), any(String.class))).thenReturn(new Authentification.Result(true, true, authenticatedUser));
        LoginManager sut = new LoginManager(
                auth,
                this.defaultFirewall,
                sessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder);
        sut.login("lydia", "lydia", "", this.defaultRequest);
        verify(sessionManager).startNewSession(authenticatedUser);
    }

    @Test(expected = PasswordChangeRequiredException.class)
    public void successfulLoginResetRequired() throws InvalidLoginException, PasswordChangeRequiredException, CaptchaRequiredException, IOException {
        User authenticatedUser = mock(User.class);
        when(authenticatedUser.getUserId()).thenReturn(5l);
        when(authenticatedUser.getIsPasswordResetRequired()).thenReturn(true);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class))).thenReturn(new Authentification.Result(true, true, authenticatedUser));
        LoginManager sut = new LoginManager(
                auth,
                this.defaultFirewall,
                this.defaultSessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder);
        sut.login("lydia", "lydia", "", this.defaultRequest);
    }

    @Test(expected = InvalidLoginException.class)
    public void failedLogin() throws InvalidLoginException, PasswordChangeRequiredException, CaptchaRequiredException, IOException {
        User authenticatedUser = mock(User.class);
        when(authenticatedUser.getUserId()).thenReturn(5l);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class))).thenReturn(new Authentification.Result(false, true, authenticatedUser));
        LoginManager sut = new LoginManager(
                auth,
                this.defaultFirewall,
                this.defaultSessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder);
        sut.login("lydia", "lydia", "", this.defaultRequest);
    }


    @Test
    public void successfulChangePassword() throws InvalidLoginException, CaptchaRequiredException, IOException {
        User authenticatedUser = mock(User.class);
        when(authenticatedUser.getUserId()).thenReturn(5l);
        when(authenticatedUser.getIsPasswordResetRequired()).thenReturn(true);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class))).thenReturn(new Authentification.Result(true, true, authenticatedUser));
        HashHelper hashHelper = mock(HashHelper.class);
        when(hashHelper.hashPassword(any(String.class))).thenReturn("hashed");
        EbeanServer s = mock(EbeanServer.class);
        LoginManager sut = new LoginManager(
                auth,
                this.defaultFirewall,
                this.defaultSessionManager,
                hashHelper,
                s,
                this.defaultLoginAttemptFinder);
        sut.changePassword("lydia", "lydia", "neuespw", "", this.defaultRequest);
        verify(authenticatedUser).setIsPasswordResetRequired(false);
        verify(authenticatedUser).setPasswordHash("hashed");
        verify(s).save(authenticatedUser);
    }

    @Test(expected = InvalidLoginException.class)
    public void failedChangePassword() throws InvalidLoginException, CaptchaRequiredException, IOException {
        User authenticatedUser = mock(User.class);
        when(authenticatedUser.getUserId()).thenReturn(5l);
        when(authenticatedUser.getIsPasswordResetRequired()).thenReturn(true);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class))).thenReturn(new Authentification.Result(false, true, authenticatedUser));
        LoginManager sut = new LoginManager(
                auth,
                this.defaultFirewall,
                this.defaultSessionManager,
                this.defaultHashHelper,
                this.defaultEbeanServer,
                this.defaultLoginAttemptFinder);
        sut.changePassword("lydia", "lydia", "neuespw", "", this.defaultRequest);
    }

    @Test(expected = CaptchaRequiredException.class)
    public void validLoginCaptchaRequired() throws InvalidLoginException, CaptchaRequiredException, PasswordChangeRequiredException, IOException {
        User authenticatedUser = mock(User.class);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class))).thenReturn(new Authentification.Result(true, true, authenticatedUser));
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
                this.defaultLoginAttemptFinder);
        sut.login("lydia", "lydia", "", this.defaultRequest);
    }


    @Test(expected = CaptchaRequiredException.class)
    public void invalidLoginCaptchaRequired() throws InvalidLoginException, CaptchaRequiredException, PasswordChangeRequiredException, IOException {
        User authenticatedUser = mock(User.class);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class))).thenReturn(new Authentification.Result(false, true, authenticatedUser));
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
                this.defaultLoginAttemptFinder);
        sut.login("lydia", "lydia", "", this.defaultRequest);
    }


    @Test(expected = InvalidLoginException.class)
    public void validLoginBanned() throws InvalidLoginException, CaptchaRequiredException, PasswordChangeRequiredException, IOException {
        User authenticatedUser = mock(User.class);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class))).thenReturn(new Authentification.Result(false, true, authenticatedUser));
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
                this.defaultLoginAttemptFinder);
        sut.login("lydia", "lydia", "", this.defaultRequest);
    }

    @Test(expected = InvalidLoginException.class)
    public void invalidLoginBannedWithCorrectLogin() throws InvalidLoginException, CaptchaRequiredException, PasswordChangeRequiredException, IOException {
        User authenticatedUser = mock(User.class);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class))).thenReturn(new Authentification.Result(true, true, authenticatedUser));
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
                this.defaultLoginAttemptFinder);
        sut.login("lydia", "lydia", "", this.defaultRequest);
    }


    @Test(expected = CaptchaRequiredException.class)
    public void validPasswordChangeCaptchaRequired() throws InvalidLoginException, CaptchaRequiredException, IOException {
        User authenticatedUser = mock(User.class);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class))).thenReturn(new Authentification.Result(true, true, authenticatedUser));
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
                this.defaultLoginAttemptFinder);
        sut.changePassword("lydia", "lydia", "neuespw", "", this.defaultRequest);
    }


    @Test(expected = CaptchaRequiredException.class)
    public void invalidPasswordChangeCaptchaRequired() throws InvalidLoginException, CaptchaRequiredException, IOException {
        User authenticatedUser = mock(User.class);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class))).thenReturn(new Authentification.Result(false, true, authenticatedUser));
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
                this.defaultLoginAttemptFinder);
        sut.changePassword("lydia", "lydia", "neuespw", "", this.defaultRequest);
    }


    @Test(expected = InvalidLoginException.class)
    public void validPasswordChangeBanned() throws InvalidLoginException, CaptchaRequiredException, IOException {
        User authenticatedUser = mock(User.class);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class))).thenReturn(new Authentification.Result(true, true, authenticatedUser));
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
                this.defaultLoginAttemptFinder);
        sut.changePassword("lydia", "lydia", "neuespw", "", this.defaultRequest);
    }


    @Test(expected = InvalidLoginException.class)
    public void invalidPasswordChangeBanned() throws InvalidLoginException, CaptchaRequiredException, IOException {
        User authenticatedUser = mock(User.class);
        Authentification auth = mock(Authentification.class);
        when(auth.Perform(any(String.class), any(String.class))).thenReturn(new Authentification.Result(false, true, authenticatedUser));
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
                this.defaultLoginAttemptFinder);
        sut.changePassword("lydia", "lydia", "neuespw", "", this.defaultRequest);
    }
}