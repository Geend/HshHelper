import io.ebean.Ebean;
import io.ebean.SqlQuery;
import io.ebean.SqlRow;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.*;
import play.Application;
import play.test.Helpers;
import policy.ext.loginFirewall.Firewall;
import policy.ext.loginFirewall.Instance;
import policy.ext.loginFirewall.Strategy;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/*
    Fake app: https://ankushthakur.com/blog/using-separate-database-for-unit-tests-in-play-framework/
 */
public class LoginFirewallTests {
    public static Application app;

    private static Long validUid = 1L;
    private static Instance fwInstanceOne;
    private static Instance fwInstanceTwo;
    private static long currentDt = 1540117115508L;

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
        DateTimeUtils.setCurrentMillisSystem();
    }

    // verschiebt den laggy ts um n schritte nach hinten
    // n = 0 keine auswirkungen
    private static void SetLaggyTs(int nBack) {
        DateTimeUtils.setCurrentMillisFixed(currentDt - (long) nBack *(3600L* (long) Firewall.LaggyTsIntervalHours *1000L));
    }

    @Test
    public void noDataLoginBypass() {
        Strategy strategy = fwInstanceOne.getStrategy(0L);
        assertThat(strategy, is(Strategy.BYPASS));
    }

    @Test
    public void loginInvalidUidBypassBorderTest() {
        int bypassBorder = Math.min(Firewall.NIPLoginsTriggerBan, Firewall.NIPLoginsTriggerVerification);
        for(int i=0; i<=bypassBorder; i++) {
            Strategy strategy = fwInstanceOne.getStrategy();

            if(i==bypassBorder)
                assertThat(strategy, not(Strategy.BYPASS));
            else
                assertThat(strategy, is(Strategy.BYPASS));

            fwInstanceOne.fail();
        }
    }

    @Test
    public void loginInvalidUidVerifyBorderTest() {
        for(int i=1; i<Firewall.NIPLoginsTriggerVerification; i++) {
            fwInstanceOne.fail();
        }

        Strategy strategy = fwInstanceOne.getStrategy();
        assertThat(strategy, is(Strategy.BYPASS));
        fwInstanceOne.fail();

        strategy = fwInstanceOne.getStrategy();
        assertThat(strategy, is(Strategy.VERIFY));
    }

    @Test
    public void loginInvalidUidBorderTest() {
        assertThat(Firewall.NIPLoginsTriggerVerification, lessThan(Firewall.NIPLoginsTriggerBan));

        Strategy strategy;

        // Keinerlei Einschränkungen solange im Limit
        for(int i=1; i<Firewall.NIPLoginsTriggerVerification; i++) {
            fwInstanceOne.fail();
            strategy = fwInstanceOne.getStrategy();
            assertThat(strategy, is(Strategy.BYPASS));

            strategy = fwInstanceTwo.getStrategy();
            assertThat(strategy, is(Strategy.BYPASS));
        }

        // Nach einem weiteren Fail ist für die eine IP verification erforderlich
        fwInstanceOne.fail();
        strategy = fwInstanceOne.getStrategy();
        assertThat(strategy, is(Strategy.VERIFY));

        // Die andere IP ist jedoch nicht betroffen.
        strategy = fwInstanceTwo.getStrategy();
        assertThat(strategy, is(Strategy.BYPASS));

        int stepsTillBan = Firewall.NIPLoginsTriggerBan - Firewall.NIPLoginsTriggerVerification;
        for(int i=1; i<stepsTillBan; i++) {
            fwInstanceOne.fail();

            // Die Stati für beide IPs verändern sich *nicht*
            strategy = fwInstanceOne.getStrategy();
            assertThat(strategy, is(Strategy.VERIFY));
            strategy = fwInstanceTwo.getStrategy();
            assertThat(strategy, is(Strategy.BYPASS));
        }

        // Noch ein failed login und die erste IP ist geblockt!
        fwInstanceOne.fail();
        strategy = fwInstanceOne.getStrategy();
        assertThat(strategy, is(Strategy.BLOCK));

        // Der Ban betrifft allerdings *nicht* andere Ips!
        strategy = fwInstanceTwo.getStrategy();
        assertThat(strategy, is(Strategy.BYPASS));
    }

    @Test
    public void loginValidUidBorderTest() {
        assertThat(Firewall.NUidLoginsTriggerVerification, lessThan(Firewall.NIPLoginsTriggerBan));
        assertThat(Firewall.NUidLoginsTriggerVerification, lessThan(Firewall.NIPLoginsTriggerVerification));

        Strategy strategy;

        // Während wir im Limit sind ist der Account frei
        for(int i=1; i<Firewall.NUidLoginsTriggerVerification; i++) {
            fwInstanceOne.fail(validUid);
            strategy = fwInstanceOne.getStrategy(validUid);
            assertThat(strategy, is(Strategy.BYPASS));

            // Das gilt auch für Zugriffe von anderen IPs!
            strategy = fwInstanceTwo.getStrategy(validUid);
            assertThat(strategy, is(Strategy.BYPASS));
        }

        // Noch ein Instance und wir benötigen verifizierung
        fwInstanceOne.fail(validUid);
        strategy = fwInstanceOne.getStrategy(validUid);
        assertThat(strategy, is(Strategy.VERIFY));
        // ..genau so wie jede andere Ip auch!
        strategy = fwInstanceTwo.getStrategy(validUid);
        assertThat(strategy, is(Strategy.VERIFY));

        // Während der weiteren Steps ist man weiterhin nur im
        // Verify modus
        int stepsTillBan = Firewall.NIPLoginsTriggerBan-Firewall.NUidLoginsTriggerVerification;
        for(int i=1; i<stepsTillBan; i++) {
            fwInstanceOne.fail(validUid);
            strategy = fwInstanceOne.getStrategy(validUid);
            assertThat(strategy, is(Strategy.VERIFY));

            // Das gilt auch für Zugriffe von anderen IPs!
            strategy = fwInstanceTwo.getStrategy(validUid);
            assertThat(strategy, is(Strategy.VERIFY));
        }

        // Noch ein Instance und dann sind wir gebanned!
        fwInstanceOne.fail(validUid);
        strategy = fwInstanceOne.getStrategy(validUid);
        assertThat(strategy, is(Strategy.BLOCK));

        // Der Ban betrifft allerdings *nicht* andere Ips!
        strategy = fwInstanceTwo.getStrategy(validUid);
        assertThat(strategy, is(Strategy.VERIFY));
    }

    @Test
    public void loginInvalidUidMultipleBucketsBorderTest() {
        assertThat(Firewall.NUidLoginsTriggerVerification, lessThan(Firewall.NIPLoginsTriggerBan));
        assertThat(Firewall.NUidLoginsTriggerVerification, lessThan(Firewall.NIPLoginsTriggerVerification));

        // Bucket now-1 erzwingen
        SetLaggyTs(1);

        Strategy strategy;

        // Bucket füllen
        for(int i=1; i<Firewall.NIPLoginsTriggerVerification; i++) {
            fwInstanceOne.fail();
        }

        // Bucket now erzwingen
        SetLaggyTs(0);

        // .. und füllen
        fwInstanceOne.fail();

        // Nur eine IP sollte verify strategy haben
        strategy = fwInstanceOne.getStrategy();
        assertThat(strategy, is(Strategy.VERIFY));
        strategy = fwInstanceTwo.getStrategy();
        assertThat(strategy, is(Strategy.BYPASS));

        // Prüfen, ob tatsächlich 2 Buckets angelegt wurden
        String sql = "SELECT * FROM loginFirewall ORDER BY laggy_dt";
        SqlQuery q = Ebean.createSqlQuery(sql);

        List<SqlRow> rows = q.findList();
        assertThat(rows.size(), is(2));
        assertThat(rows.get(0).getInteger("count"), is(Firewall.NIPLoginsTriggerVerification-1));
        assertThat(rows.get(1).getInteger("count"), is(1));
    }

    @Test
    public void garbageCollectorTest() {
        int bucketsToExpiry = 5;

        for(int i=0; i<Firewall.RelevantHours+bucketsToExpiry; i++) {
            SetLaggyTs(i);
            fwInstanceOne.fail();
        }

        String sql = "SELECT * FROM loginFirewall";
        SqlQuery q = Ebean.createSqlQuery(sql);
        List<SqlRow> rows = q.findList();
        assertThat(rows.size(), is(Firewall.RelevantHours+bucketsToExpiry));

        SetLaggyTs(0);
        Firewall.GarbageCollect();

        sql = "SELECT * FROM loginFirewall ORDER BY laggy_dt DESC";
        q = Ebean.createSqlQuery(sql);
        rows = q.findList();
        assertThat(rows.size(), is(Firewall.RelevantHours));

        for(int i=0; i<rows.size(); i++) {
            SetLaggyTs(i);
            DateTime rowDt = new DateTime(rows.get(i).getLong("laggy_dt"));
            DateTime currentDt = Firewall.GetLaggyDT();
            assertThat(rowDt, is(currentDt));
        }
    }
}
