import org.junit.*;
import play.Application;
import play.test.Helpers;
import policy.ext.loginFirewall.Firewall;
import policy.ext.loginFirewall.Login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

/*
    Fake app: https://ankushthakur.com/blog/using-separate-database-for-unit-tests-in-play-framework/
 */
public class LoginFirewallTests {
    public static Application app;

    private static Long validUid = 1L;
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

    @Test
    public void loginInvalidUidBypassBorderTest() {
        int bypassBorder = Math.min(Firewall.NIPLoginsTriggerBan, Firewall.NIPLoginsTriggerVerification);
        for(int i=0; i<=bypassBorder; i++) {
            Login.Strategy strategy = fwInstanceOne.getStrategy();

            if(i==bypassBorder)
                assertThat(strategy, not(Login.Strategy.BYPASS));
            else
                assertThat(strategy, is(Login.Strategy.BYPASS));

            fwInstanceOne.fail();
        }
    }

    @Test
    public void loginInvalidUidVerifyBorderTest() {
        for(int i=1; i<Firewall.NIPLoginsTriggerVerification; i++) {
            fwInstanceOne.fail();
        }

        Login.Strategy strategy = fwInstanceOne.getStrategy();
        assertThat(strategy, is(Login.Strategy.BYPASS));
        fwInstanceOne.fail();

        strategy = fwInstanceOne.getStrategy();
        assertThat(strategy, is(Login.Strategy.VERIFY));
    }

    @Test
    public void loginInvalidUidBorderTest() {
        assertThat(Firewall.NIPLoginsTriggerVerification, lessThan(Firewall.NIPLoginsTriggerBan));

        Login.Strategy strategy;

        // Keinerlei Einschränkungen solange im Limit
        for(int i=1; i<Firewall.NIPLoginsTriggerVerification; i++) {
            fwInstanceOne.fail();
            strategy = fwInstanceOne.getStrategy();
            assertThat(strategy, is(Login.Strategy.BYPASS));

            strategy = fwInstanceTwo.getStrategy();
            assertThat(strategy, is(Login.Strategy.BYPASS));
        }

        // Nach einem weiteren Fail ist für die eine IP verification erforderlich
        fwInstanceOne.fail();
        strategy = fwInstanceOne.getStrategy();
        assertThat(strategy, is(Login.Strategy.VERIFY));

        // Die andere IP ist jedoch nicht betroffen.
        strategy = fwInstanceTwo.getStrategy();
        assertThat(strategy, is(Login.Strategy.BYPASS));

        int stepsTillBan = Firewall.NIPLoginsTriggerBan - Firewall.NIPLoginsTriggerVerification;
        for(int i=1; i<stepsTillBan; i++) {
            fwInstanceOne.fail();

            // Die Stati für beide IPs verändern sich *nicht*
            strategy = fwInstanceOne.getStrategy();
            assertThat(strategy, is(Login.Strategy.VERIFY));
            strategy = fwInstanceTwo.getStrategy();
            assertThat(strategy, is(Login.Strategy.BYPASS));
        }

        // Noch ein failed login und die erste IP ist geblockt!
        fwInstanceOne.fail();
        strategy = fwInstanceOne.getStrategy();
        assertThat(strategy, is(Login.Strategy.BLOCK));

        // Der Ban betrifft allerdings *nicht* andere Ips!
        strategy = fwInstanceTwo.getStrategy();
        assertThat(strategy, is(Login.Strategy.BYPASS));
    }

    @Test
    public void loginValidUidBorderTest() {
        assertThat(Firewall.NUidLoginsTriggerVerification, lessThan(Firewall.NIPLoginsTriggerBan));
        assertThat(Firewall.NUidLoginsTriggerVerification, lessThan(Firewall.NIPLoginsTriggerVerification));

        Login.Strategy strategy;

        // Während wir im Limit sind ist der Account frei
        for(int i=1; i<Firewall.NUidLoginsTriggerVerification; i++) {
            fwInstanceOne.fail(validUid);
            strategy = fwInstanceOne.getStrategy(validUid);
            assertThat(strategy, is(Login.Strategy.BYPASS));

            // Das gilt auch für Zugriffe von anderen IPs!
            strategy = fwInstanceTwo.getStrategy(validUid);
            assertThat(strategy, is(Login.Strategy.BYPASS));
        }

        // Noch ein Login und wir benötigen verifizierung
        fwInstanceOne.fail(validUid);
        strategy = fwInstanceOne.getStrategy(validUid);
        assertThat(strategy, is(Login.Strategy.VERIFY));
        // ..genau so wie jede andere Ip auch!
        strategy = fwInstanceTwo.getStrategy(validUid);
        assertThat(strategy, is(Login.Strategy.VERIFY));

        // Während der weiteren Steps ist man weiterhin nur im
        // Verify modus
        int stepsTillBan = Firewall.NIPLoginsTriggerBan-Firewall.NUidLoginsTriggerVerification;
        for(int i=1; i<stepsTillBan; i++) {
            fwInstanceOne.fail(validUid);
            strategy = fwInstanceOne.getStrategy(validUid);
            assertThat(strategy, is(Login.Strategy.VERIFY));

            // Das gilt auch für Zugriffe von anderen IPs!
            strategy = fwInstanceTwo.getStrategy(validUid);
            assertThat(strategy, is(Login.Strategy.VERIFY));
        }

        // Noch ein Login und dann sind wir gebanned!
        fwInstanceOne.fail(validUid);
        strategy = fwInstanceOne.getStrategy(validUid);
        assertThat(strategy, is(Login.Strategy.BLOCK));

        // Der Ban betrifft allerdings *nicht* andere Ips!
        strategy = fwInstanceTwo.getStrategy(validUid);
        assertThat(strategy, is(Login.Strategy.VERIFY));
    }

    // TODO: testen von mehreren buckets
    // TODO: garbage collector testen
}
