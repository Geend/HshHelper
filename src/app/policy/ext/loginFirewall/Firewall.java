package policy.ext.loginFirewall;

import io.ebean.Ebean;
import io.ebean.SqlUpdate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

public class Firewall {
    public static int LaggyTsIntervalHours = 1; // 1 Std
    public static int RelevantHours = 5;
    public static int NIPLoginsTriggerVerification = 50;
    public static int NIPLoginsTriggerBan = 100;
    public static int NUidLoginsTriggerVerification = 5;

    public static void Initialize() {
        String createTableSql =
            "CREATE TABLE IF NOT EXISTS loginFirewall (\n" +
                "ident VARCHAR(255) NOT NULL,\n" +
                "laggy_dt DATETIME NOT NULL,\n" +
                "count BIGINT unsigned NOT NULL,\n" +
                "expiry DATETIME NOT NULL,\n" +
                "PRIMARY KEY (ident, laggy_dt)\n"+
            ");";

        String createDTIndexSql = "CREATE INDEX IF NOT EXISTS loginFirewallExpiry\n" +
                "ON loginFirewall (expiry);";

        Ebean.createSqlUpdate(createTableSql).execute();
        Ebean.createSqlUpdate(createDTIndexSql).execute();
    }

    public static void GarbageCollect() {
        String cleanSql =
            "DELETE FROM loginFirewall WHERE expiry <= :current";

        SqlUpdate upd = Ebean.createSqlUpdate(cleanSql);
        upd.setParameter("current", GetLaggyDT());
        upd.execute();
    }

    public static void Flush() {
        String cleanSql =
            "DELETE FROM loginFirewall";
        Ebean.createSqlUpdate(cleanSql).execute();
    }

    public static Instance Get(String remoteIp) {
        return new Instance(remoteIp);
    }

    public static DateTime GetLaggyDT() {
        long unixTime = DateTimeUtils.currentTimeMillis() / 1000L;
        unixTime = unixTime / (3600L*Firewall.LaggyTsIntervalHours); // laggen. ts soll nur in intervallen ansteigen
        unixTime = unixTime * (3600L*Firewall.LaggyTsIntervalHours); // alles was zwischen diesen intervallen liegt
        unixTime = unixTime * 1000L;                                 // soll verworfen werden! -> ganzzahl-division!
        return new DateTime(unixTime);
    }
}
