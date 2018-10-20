import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import policy.ext.loginFirewall.Firewall;
import policy.ext.loginFirewall.Login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class LoginFirewallTests {
    private static Login fwInstanceOne;
    private static Login fwInstanceTwo;

    @BeforeClass
    public static void setup() {
        Firewall.Initialize();
        Firewall.Flush();

        fwInstanceOne = Firewall.Get("12.21.12.21");
        fwInstanceTwo = Firewall.Get("21.21.12.21");
    }

    @AfterClass
    public static void tearDown() {
        Firewall.Flush();
    }

    @Test
    public void noDataLoginBypass() {
        Login.Strategy strategy = fwInstanceOne.getStrategy(0L);
        assertThat(strategy, is(Login.Strategy.BYPASS));
    }
}
