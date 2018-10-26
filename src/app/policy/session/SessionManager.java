package policy.session;

import models.User;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import play.mvc.Http;
import policy.ConstraintValues;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SessionManager {
    private static final String CtxCurrentSession = "CurrentSession";
    private static final String CookieSessionName = "HsHSession";

    public static void StartNewSession(User user) {
        Http.Context ctx = Http.Context.current();

        Session dbs = new Session();
        dbs.setRemoteAddress(ctx.request().remoteAddress());
        dbs.setIssuedAt(DateTime.now());
        dbs.setUser(user);
        dbs.save();

        ctx.session().put(CookieSessionName, dbs.getSessionKey().toString());
    }

    private static Session CurrentSession() {
        Http.Context ctx = Http.Context.current();
        if(ctx.args.containsKey(CtxCurrentSession)) {
            return (Session)ctx.args.get(CtxCurrentSession);
        }

        Session session = null;
        String sessionId = ctx.session().getOrDefault(CookieSessionName, null);
        if(!StringUtils.isEmpty(sessionId)) {
            UUID sessionUid = UUID.fromString(sessionId);
            Session dbs = Session.finder.byId(sessionUid);
            if(dbs != null && dbs.getUser() != null) {
                // IP Addressen müssen matchen && Session darf nicht zu alt sein!
                if(dbs.getRemoteAddress().equals(ctx.request().remoteAddress()) &&
                    dbs.getIssuedAt().plus(ConstraintValues.SESSION_TIMEOUT_HOURS).isAfterNow()) {
                        session = dbs;
                }
            }
        }

        ctx.args.put(CtxCurrentSession, session);
        return session;
    }

    public static User CurrentUser() {
        if(!HasActiveSession()) {
            throw new RuntimeException("There is no Session that is destroyable");
        }

        return CurrentSession().getUser();
    }

    public static void DestroyCurrentSession() {
        if(!HasActiveSession()) {
            throw new RuntimeException("There is no Session that is destroyable");
        }

        Session current = CurrentSession();
        current.delete();
        Http.Context.current().session().remove(CookieSessionName);

        // TODO: Nachdenken ob Entfernung aus Kontext Sinn macht?
    }

    public static List<UserSession> SessionsByUser(User user) {
        List<Session> sessions = Session.finder.query().where().eq("user", user).findList();
        List<UserSession> result = new ArrayList<>();

        for(Session s : sessions) {
            result.add(new UserSession(s));
        }

        return result;
    }

    public static boolean HasActiveSession() {
        return CurrentSession() != null;
    }
}
