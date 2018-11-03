package policy.ext.loginFirewall;

import io.ebean.Ebean;
import io.ebean.SqlQuery;
import io.ebean.SqlRow;
import io.ebean.SqlUpdate;
import org.joda.time.DateTime;

public class Instance {
    private String remoteIp;

    protected Instance(String remoteIp) {
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

    private void incrKey(String key) {
        DateTime laggyDt = LaggyDT.Get();

        String incrSql =
            "INSERT INTO loginFirewall (ident, laggy_dt, count, expiry) \n"+
            "VALUES(:ident, :laggy_dt, :one, :expiry_dt) \n"+
            "ON DUPLICATE KEY UPDATE count = count + :one";
        SqlUpdate upd = Ebean.createSqlUpdate(incrSql);
        upd.setParameter("ident", key);
        upd.setParameter("laggy_dt", laggyDt);
        upd.setParameter("expiry_dt", laggyDt.plusHours(Firewall.RelevantHours));
        upd.setParameter("one", 1);
        upd.execute();
    }

    private long getKeyCount(String key) {
        String countSql =
            "SELECT nvl(sum(count),ZERO()) as count FROM loginFirewall WHERE ident=:ident AND laggy_dt > :dt_lower_bound";

        DateTime lowerBound = LaggyDT.Get();
        lowerBound = lowerBound.minusHours(Firewall.RelevantHours);

        SqlQuery qry = Ebean.createSqlQuery(countSql);
        qry.setParameter("ident", key);
        qry.setParameter("dt_lower_bound", lowerBound);
        SqlRow row = qry.findOne();

        return row.getLong("count");
    }
}
