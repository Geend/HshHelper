package policy.ext.loginFirewall;

import io.ebean.Ebean;
import io.ebean.SqlQuery;
import io.ebean.SqlRow;
import io.ebean.SqlUpdate;
import org.joda.time.DateTime;

public class Login {
    public enum Strategy {
        BYPASS,
        VERIFY,
        BLOCK
    };

    private String remoteIp;

    protected Login(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public Strategy getStrategy() {
        return getStrategy(null);
    }

    public Strategy getStrategy(Long uid) {
        long accountFailedLogins = 0;
        long ipFailedLogins = getKeyCount(remoteIp);

        if(uid != null) {
            accountFailedLogins = getKeyCount(uid.toString());
        }

        if(ipFailedLogins >= Firewall.NIPLoginsTriggerBan) {
            return Strategy.BLOCK;
        }

        if(ipFailedLogins >= Firewall.NIPLoginsTriggerVerification) {
            return Strategy.VERIFY;
        }

        if(accountFailedLogins >= Firewall.NUidLoginsTriggerVerification) {
            return Strategy.VERIFY;
        }

        return Strategy.BYPASS;
    }

    public void fail() {
        this.fail(null);
    }

    public void fail(Long uid) {
        if(uid != null) {
            incrKey(uid.toString());
        }
        incrKey(remoteIp);
    }

    private DateTime getLaggyDT() {
        long unixTime = System.currentTimeMillis() / 1000L;
        unixTime = unixTime / (3600L*Firewall.LaggyTsIntervalHours); // laggen. ts soll nur in intervallen ansteigen
        unixTime = unixTime * (3600L*Firewall.LaggyTsIntervalHours); // alles was zwischen diesen intervallen liegt
        unixTime = unixTime * 1000L;                                 // soll verworfen werden! -> ganzzahl-division!
        return new DateTime(unixTime);
    }

    private void incrKey(String key) {
        String incrSql =
            "INSERT INTO loginFirewall (ident, laggy_dt, count, expiry) \n"+
            "VALUES(:ident, :laggy_dt, 1, TIMESTAMPADD(HOUR, :expiry_hours, CURRENT_TIMESTAMP)) \n"+
            "ON DUPLICATE KEY UPDATE count = count + 1";
        SqlUpdate upd = Ebean.createSqlUpdate(incrSql);
        upd.setParameter("ident", key);
        upd.setParameter("laggy_dt", getLaggyDT());
        upd.setParameter("expiry_hours", Firewall.RelevantHours);
        upd.execute();
    }

    private long getKeyCount(String key) {
        String countSql =
            "SELECT nvl(sum(count),0) as count FROM loginFirewall WHERE ident=:ident AND laggy_dt > :dt_lower_bound";

        DateTime lowerBound = getLaggyDT();
        lowerBound = lowerBound.minusHours(Firewall.RelevantHours);

        SqlQuery qry = Ebean.createSqlQuery(countSql);
        qry.setParameter("ident", key);
        qry.setParameter("dt_lower_bound", lowerBound);
        SqlRow row = qry.findOne();

        return row.getLong("count");
    }
}
