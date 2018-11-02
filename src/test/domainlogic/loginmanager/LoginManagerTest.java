package domainlogic.loginmanager;

import extension.HashHelper;
import io.ebean.Ebean;
import models.User;
import models.finders.UserFinder;
import org.junit.*;
import play.Application;
import play.mvc.Http;
import play.test.Helpers;
import policy.ext.loginFirewall.Firewall;
import policy.ext.loginFirewall.Instance;
import policy.ext.loginFirewall.Strategy;
import policy.session.SessionManager;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoginManagerTest {
    public static Application app;
    public static User lydia;
    public static User annika;
    public static HashHelper hashHelper = new HashHelper();

    @AfterClass
    public static void stopApp() {
        Helpers.stop(app);
    }

    @BeforeClass
    public static void setupGlobal() {
        app = Helpers.fakeApplication();
        Helpers.start(app);
    }

    UserFinder userFinder;
    LoginManager loginManager;
    Firewall firewall;
    SessionManager sessionManager;

    Firewall alwaysCaptchaFirewall;
    Firewall alwaysBannedFirewall;

    @Before
    public void setup() {
        firewall = new Firewall();
        sessionManager = new SessionManager();
        loginManager = new LoginManager(hashHelper, firewall, sessionManager);
        userFinder = new UserFinder();

        Instance alwaysBannedInstance = mock(Instance.class);
        when(alwaysBannedInstance.getStrategy()).thenReturn(Strategy.BLOCK);
        when(alwaysBannedInstance.getStrategy(anyLong())).thenReturn(Strategy.BLOCK);

        alwaysBannedFirewall = mock(Firewall.class);
        when(alwaysBannedFirewall.get(anyString())).thenReturn(alwaysBannedInstance);

        Instance alwaysCaptchaInstance = mock(Instance.class);
        when(alwaysCaptchaInstance.getStrategy()).thenReturn(Strategy.VERIFY);
        when(alwaysCaptchaInstance.getStrategy(anyLong())).thenReturn(Strategy.VERIFY);

        alwaysCaptchaFirewall = mock(Firewall.class);
        when(alwaysCaptchaFirewall.get(anyString())).thenReturn(alwaysCaptchaInstance);

        Http.Request request = Helpers.fakeRequest("GET", "/").remoteAddress("1.2.23.4").build();
        Http.Context.current.set(Helpers.httpContext(request));


        // PW reset nicht required
        lydia = new User("lydia", "hsh.helper+lydia@gmail.com", hashHelper.hashPassword("lydia"), false, 10);
        lydia.save();

        // PW reset required
        annika = new User("annika", "hsh.helper+annika@gmail.com", hashHelper.hashPassword("annika"), true, 10);
        annika.save();
    }

    @After
    public void teardown() {
        Ebean.createSqlUpdate("DELETE FROM internal_session").execute();

        lydia.delete();
        annika.delete();
    }

    @Test
    public void successfulLogin() throws InvalidLoginException, PasswordChangeRequiredException, CaptchaRequiredException {
        loginManager.login(
                "lydia", "lydia", ""
        );
    }

    @Test(expected = PasswordChangeRequiredException.class)
    public void successfulLoginResetRequired() throws InvalidLoginException, PasswordChangeRequiredException, CaptchaRequiredException {
        loginManager.login(
                "annika", "annika", ""
        );
    }

    @Test(expected = InvalidLoginException.class)
    public void failedLogin() throws InvalidLoginException, PasswordChangeRequiredException, CaptchaRequiredException {
        loginManager.login(
                "lydia", "lydiaxxxxxxxxx", ""
        );
    }

    @Test
    public void successfulChangePassword() throws InvalidLoginException, CaptchaRequiredException {
        loginManager.changePassword(
                "annika", "annika", "neuesPw", ""
        );

        User newAnnika = userFinder.byId(annika.getUserId());

        assertThat(
            newAnnika.getIsPasswordResetRequired(),
            is(false)
        );

        assertTrue(
            hashHelper.checkHash(
                "neuesPw",
                newAnnika.getPasswordHash()
            )
        );
    }

    @Test(expected = InvalidLoginException.class)
    public void failedChangePassword() throws InvalidLoginException, CaptchaRequiredException {
        loginManager.changePassword(
                "annika", "xx", "neuesPw", ""
        );
    }

    @Test(expected = CaptchaRequiredException.class)
    public void validLoginCaptchaRequired() throws InvalidLoginException, CaptchaRequiredException, PasswordChangeRequiredException {
        LoginManager lm = new LoginManager(hashHelper, alwaysCaptchaFirewall, sessionManager);
        lm.login(
                "lydia", "lydia", ""
        );
    }

    @Test(expected = CaptchaRequiredException.class)
    public void invalidLoginCaptchaRequired() throws InvalidLoginException, CaptchaRequiredException, PasswordChangeRequiredException {
        LoginManager lm = new LoginManager(hashHelper, alwaysCaptchaFirewall, sessionManager);
        lm.login(
            "lydia", "lydiaxxxxxx", ""
        );
    }

    @Test(expected = InvalidLoginException.class)
    public void validLoginBanned() throws InvalidLoginException, CaptchaRequiredException, PasswordChangeRequiredException {
        LoginManager lm = new LoginManager(hashHelper, alwaysBannedFirewall, sessionManager);
        lm.login(
                "lydia", "lydia", ""
        );
    }

    @Test(expected = InvalidLoginException.class)
    public void invalidLoginBanned() throws InvalidLoginException, CaptchaRequiredException, PasswordChangeRequiredException {
        LoginManager lm = new LoginManager(hashHelper, alwaysBannedFirewall, sessionManager);
        lm.login(
                "lydia", "lydiaxxxx", ""
        );
    }

    @Test(expected = CaptchaRequiredException.class)
    public void validPasswordChangeCaptchaRequired() throws InvalidLoginException, CaptchaRequiredException {
        LoginManager lm = new LoginManager(hashHelper, alwaysCaptchaFirewall, sessionManager);
        lm.changePassword(
                "annika", "annika", "neuesPw", ""
        );
    }

    @Test(expected = CaptchaRequiredException.class)
    public void invalidPasswordChangeCaptchaRequired() throws InvalidLoginException, CaptchaRequiredException {
        LoginManager lm = new LoginManager(hashHelper, alwaysCaptchaFirewall, sessionManager);
        lm.changePassword(
                "annika", "annixxka", "neuesPw", ""
        );
    }

    @Test(expected = InvalidLoginException.class)
    public void validPasswordChangeBanned() throws InvalidLoginException, CaptchaRequiredException {
        LoginManager lm = new LoginManager(hashHelper, alwaysBannedFirewall, sessionManager);
        lm.changePassword(
                "annika", "annika", "neuesPw", ""
        );
    }

    @Test(expected = InvalidLoginException.class)
    public void invalidPasswordChangeBanned() throws InvalidLoginException, CaptchaRequiredException {
        LoginManager lm = new LoginManager(hashHelper, alwaysBannedFirewall, sessionManager);
        lm.changePassword(
                "annika", "annikxxxa", "neuesPw", ""
        );
    }
}