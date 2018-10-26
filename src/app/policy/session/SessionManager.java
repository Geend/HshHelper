package policy.session;

import models.User;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import play.mvc.Http;
import policy.ConstraintValues;

import java.util.UUID;

public class SessionManager {
    private static final String CtxSessionUser = "CurrentSessionUser";
    private static final String CookieSessionName = "HsHSession";

    public static void StartNewSession(User user) {
        Http.Context ctx = Http.Context.current();

        Session dbs = new Session();
        dbs.setRemoteAddress(ctx.request().remoteAddress());
        dbs.setIssuedAt(DateTime.now());
        dbs.setUser(user);
        dbs.save();

        // .. cookie
    }

    public static User CurrentUser() {
        Http.Context ctx = Http.Context.current();
        if(ctx.args.containsKey(CtxSessionUser)) {
            return (User)ctx.args.get(CtxSessionUser);
        }

        User user = null;
        String sessionId = ctx.session().getOrDefault(CookieSessionName, null);
        if(!StringUtils.isEmpty(sessionId)) {
            UUID sessionUid = UUID.fromString(sessionId);
            Session dbs = Session.finder.byId(sessionUid);
            if(dbs != null && dbs.getUser() != null) {
                // IP Addressen müssen matchen && Session darf nicht zu alt sein!
                if(dbs.getRemoteAddress().equals(ctx.request().remoteAddress()) &&
                    dbs.getIssuedAt().plus(ConstraintValues.SESSION_TIMEOUT_HOURS).isAfterNow()) {
                        user = dbs.getUser();
                }
            }
        }

        ctx.args.put(CtxSessionUser, user);
        return user;
    }

    public static boolean HasActiveSession() {
        return CurrentUser() != null;
    }
}
