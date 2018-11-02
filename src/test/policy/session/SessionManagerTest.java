package policy.session;

import extension.HashHelper;
import models.User;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import play.Application;
import play.mvc.Http;
import play.test.Helpers;
import policy.ext.loginFirewall.Firewall;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class SessionManagerTest {
    public static Application app;
    public static SessionManager sessionManager;
    public static User robin;
    public static HashHelper hashHelper;

    public static String defaultIp = "1.2.23.4";
    public static String otherIp = "2.2.2.2";

    @BeforeClass
    public static void startApp() {
        app = Helpers.fakeApplication();
        Helpers.start(app);

        hashHelper = new HashHelper();
        sessionManager = new SessionManager();

        robin = new User("robin", "hsh.helper+robin@gmail.com", hashHelper.hashPassword("robin"), true, 10);
        robin.save();
    }

    @AfterClass
    public static void stopApp() {
        Helpers.stop(app);
    }

    @Before
    public void setup() {
        setIp(defaultIp);
    }

    private void setIp(String ip) {
        Http.Request request = Helpers.fakeRequest("GET", "/").remoteAddress(ip).build();
        Http.Context.current.set(Helpers.httpContext(request));
    }

    @Test
    public void sessionLifespan() {
        assertThat(
            sessionManager.hasActiveSession(),
            is(false)
        );

        sessionManager.startNewSession(robin);

        assertThat(
            sessionManager.hasActiveSession(),
            is(true)
        );

        assertThat(
            sessionManager.currentUser(),
            is(robin)
        );

        sessionManager.destroyCurrentSession();

        assertThat(
            sessionManager.hasActiveSession(),
            is(false)
        );
    }

    @Test
    public void sessionLifespanIpChange() {
        sessionManager.startNewSession(robin);

        assertThat(
            sessionManager.hasActiveSession(),
            is(true)
        );

        setIp(otherIp);

        assertThat(
            sessionManager.hasActiveSession(),
            is(false)
        );
    }



}