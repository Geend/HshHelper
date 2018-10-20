import org.junit.*;
import play.Application;
import play.test.Helpers;
import policy.ext.loginFirewall.Firewall;
import policy.ext.loginFirewall.Login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/*
    Fake app: https://ankushthakur.com/blog/using-separate-database-for-unit-tests-in-play-framework/
 */
public class LoginFirewallTests {
    public static Application app;

    private static Login fwInstanceOne;
    private static Login fwInstanceTwo;

    @BeforeClass
    public static void startApp() {
        app = Helpers.fakeApplication();
        Helpers.start(app);
    }

    @AfterClass
    public static void stopApp() {
        Helpers.stop(app);
    }

    @Before
    public void setup() {
        Firewall.Initialize();
        Firewall.Flush();

        fwInstanceOne = Firewall.Get("12.21.12.21");
        fwInstanceTwo = Firewall.Get("21.21.12.21");
    }

    @After
    public void tearDown() {
        Firewall.Flush();
    }

    @Test
    public void noDataLoginBypass() {
        Login.Strategy strategy = fwInstanceOne.getStrategy(0L);
        assertThat(strategy, is(Login.Strategy.BYPASS));
    }
}
