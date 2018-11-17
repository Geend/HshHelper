package policyenforcement.ext.loginFirewall;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

public class LaggyDT {
    public static DateTime Get() {
        long unixTime = DateTimeUtils.currentTimeMillis() / 1000L;
        unixTime = unixTime / (3600L*Firewall.LaggyTsIntervalHours); // laggen. ts soll nur in intervallen ansteigen
        unixTime = unixTime * (3600L*Firewall.LaggyTsIntervalHours); // alles was zwischen diesen intervallen liegt
        unixTime = unixTime * 1000L;                                 // soll verworfen werden! -> ganzzahl-division!
        return new DateTime(unixTime);
    }
}
