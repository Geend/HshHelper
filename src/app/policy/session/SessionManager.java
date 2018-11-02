package policy.session;

import models.User;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import play.Logger;
import play.mvc.Http;
import policy.ConstraintValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SessionManager {
    private static final String CtxCurrentSession = "CurrentSession";
    private static final String CookieSessionName = "HsHSession";

    public static void StartNewSession(User user) {
        Http.Context ctx = Http.Context.current();

        InternalSession dbs = new InternalSession();
        dbs.setRemoteAddress(ctx.request().remoteAddress());
        dbs.setIssuedAt(DateTime.now());
        dbs.setUser(user);
        dbs.save();

        ctx.session().put(CookieSessionName, dbs.getSessionKey().toString());
    }

    private static InternalSession CurrentSession() {
        Http.Context ctx = Http.Context.current();
        if(ctx.args.containsKey(CtxCurrentSession)) {
            return (InternalSession)ctx.args.get(CtxCurrentSession);
        }

        InternalSession session = null;
        String sessionId = ctx.session().getOrDefault(CookieSessionName, null);
        if(!StringUtils.isEmpty(sessionId)) {
            UUID sessionUid = UUID.fromString(sessionId);
            InternalSession dbs = InternalSession.finder.byId(sessionUid);
            if(dbs != null && dbs.getUser() != null) {
                // IP Addressen müssen matchen && InternalSession darf nicht zu alt sein!
                if(dbs.getRemoteAddress().equals(ctx.request().remoteAddress()) &&
                    dbs.getIssuedAt().plus(ConstraintValues.SESSION_TIMEOUT_HOURS).isBeforeNow()) {
                        session = dbs;
                }
            }
        }

        ctx.args.put(CtxCurrentSession, session);
        return session;
    }

    public static User CurrentUser() {
        if(!HasActiveSession()) {
            throw new RuntimeException("There is no InternalSession that is destroyable");
        }

        return CurrentSession().getUser();
    }

    public static void DestroyCurrentSession() {
        if(!HasActiveSession()) {
            throw new RuntimeException("There is no InternalSession that is destroyable");
        }

        InternalSession current = CurrentSession();
        current.delete();
        Http.Context.current().session().remove(CookieSessionName);

        // TODO: Nachdenken ob Entfernung aus Kontext Sinn macht?
    }

    public static List<Session> SessionsByUser(User user) {
        List<InternalSession> sessions = InternalSession.finder.query().where().eq("user", user).findList();
        List<Session> result = new ArrayList<>();

        for(InternalSession s : sessions) {
            result.add(new Session(s));
        }

        return result;
    }

    public static Optional<Session> GetUserSession(User user, UUID sessionKey) {
        Optional<InternalSession> session = InternalSession.finder.query().where().eq("user", user).eq("sessionKey", sessionKey).findOneOrEmpty();

        Optional<Session> ret = Optional.empty();
        if(session.isPresent()) {
            ret = Optional.of(new Session(session.get()));
        }

        return ret;
    }

    public static boolean HasActiveSession() {
        return CurrentSession() != null;
    }

    public static void GarbageCollect() {
        int deletedSessions = InternalSession.finder.query().where()
            .lt("issuedAt", DateTime.now().minusHours(ConstraintValues.SESSION_TIMEOUT_HOURS))
            .delete();

        Logger.info("REWRITE/ Delete "+deletedSessions+" Sessions");
    }
}
