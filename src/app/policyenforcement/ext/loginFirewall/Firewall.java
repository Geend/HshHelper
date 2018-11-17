package policyenforcement.ext.loginFirewall;

import io.ebean.Ebean;
import io.ebean.SqlUpdate;

import javax.inject.Singleton;

@Singleton
public class Firewall {
    public static int LaggyTsIntervalHours = 1; // 1 Std
    public static int RelevantHours = 5;
    public static int NIPLoginsTriggerVerification = 50;
    public static int NIPLoginsTriggerBan = 100;
    public static int NUidLoginsTriggerVerification = 5;

    public Firewall() {
        this.initialize();
    }

    public void initialize() {
        String createTableSql =
            "CREATE TABLE IF NOT EXISTS loginFirewall (\n" +
                "ident VARCHAR NOT NULL,\n" +
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

    public void garbageCollect() {
        String cleanSql =
            "DELETE FROM loginFirewall WHERE expiry <= :current";

        SqlUpdate upd = Ebean.createSqlUpdate(cleanSql);
        upd.setParameter("current", LaggyDT.Get());
        upd.execute();
    }

    public void flush() {
        String cleanSql =
            "DELETE FROM loginFirewall";
        Ebean.createSqlUpdate(cleanSql).execute();
    }

    public Instance get(String remoteIp) {
        return new Instance(remoteIp);
    }
}
