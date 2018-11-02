package domainlogic.loginmanager;

import extension.HashHelper;
import models.User;
import models.finders.UserFinder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import play.Application;
import play.mvc.Http;
import play.test.Helpers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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

        // PW reset nicht required
        lydia = new User("lydia", "hsh.helper+lydia@gmail.com", hashHelper.hashPassword("lydia"), false, 10);
        lydia.save();

        // PW reset required
        annika = new User("annika", "hsh.helper+annika@gmail.com", hashHelper.hashPassword("annika"), true, 10);
        annika.save();
    }

    UserFinder userFinder;
    LoginManager loginManager;

    @Before
    public void setup() {
        loginManager = new LoginManager(hashHelper);
        userFinder = new UserFinder();

        Http.Request request = Helpers.fakeRequest("GET", "/").remoteAddress("1.2.23.4").build();
        Http.Context.current.set(Helpers.httpContext(request));
    }

    @Test
    public void successfulLogin() throws InvalidUsernameOrPasswordException, PasswordChangeRequiredException, CaptchaRequiredException {
        loginManager.login(
                "lydia", "lydia", ""
        );
    }

    @Test(expected = InvalidUsernameOrPasswordException.class)
    public void failedLogin() throws InvalidUsernameOrPasswordException, PasswordChangeRequiredException, CaptchaRequiredException {
        loginManager.login(
                "lydia", "lydiaxxxxxxxxx", ""
        );
    }

    @Test
    public void successfulChangePassword() throws InvalidUsernameOrPasswordException, CaptchaRequiredException {
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

    @Test(expected = InvalidUsernameOrPasswordException.class)
    public void failedChangePassword() throws InvalidUsernameOrPasswordException, CaptchaRequiredException {
        loginManager.changePassword(
                "annika", "xx", "neuesPw", ""
        );
    }
}