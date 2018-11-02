package domainlogic.loginmanager;

import domainlogic.usermanager.UserManager;
import extension.HashHelper;
import models.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import play.Application;
import play.test.Helpers;

import static org.junit.Assert.*;

public class LoginManagerTest {
    public static Application app;
    public static User klaus;
    public static User peter;

    @AfterClass
    public static void stopApp() {
        peter.delete();
        klaus.delete();
        Helpers.stop(app);
    }

    @BeforeClass
    public static void setupGlobal() {
        app = Helpers.fakeApplication();
        Helpers.start(app);

        // PW reset nicht required
        klaus = new User("klaus", "hsh.helper+klaus@gmail.com", HashHelper.hashPassword("klaus"), false, 10);
        klaus.save();

        // PW reset required
        peter = new User("peter", "hsh.helper+peter@gmail.com", HashHelper.hashPassword("peter"), true, 10);
        peter.save();
    }

    @Test
    public void login() throws InvalidUsernameOrPasswordException, PasswordChangeRequiredException, CaptchaRequiredException {
        LoginManager loginManager = new LoginManager();

        loginManager.login(
            "klaus", "klaus", ""
        );
    }

    @Test
    public void changePassword() {
    }

    @Test
    public void logout() {
    }
}