package policy.session;

import models.User;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import play.Logger;
import play.mvc.Http;
import policy.ConstraintValues;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class SessionManager {
    // Hack for templates
    public static SessionManager SessionInstance() {
        return new SessionManager();
    }

    private static final String CtxCurrentSession = "CurrentSession";
    private static final String CookieSessionName = "HsHSession";

    public void startNewSession(User user) {
        Http.Context ctx = Http.Context.current();

        InternalSession dbs = new InternalSession();
        dbs.setRemoteAddress(ctx.request().remoteAddress());
        dbs.setIssuedAt(DateTime.now());
        dbs.setUser(user);
        dbs.save();

        ctx.session().put(CookieSessionName, dbs.getSessionKey().toString());
        ctx.args.remove(CtxCurrentSession);
    }

    private InternalSession currentSession() {
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
                        dbs.getIssuedAt().plusHours(ConstraintValues.SESSION_TIMEOUT_HOURS).isAfterNow()) {
                    session = dbs;
                }
            }
        }

        ctx.args.put(CtxCurrentSession, session);
        return session;
    }

    public User currentUser() {
        if(!hasActiveSession()) {
            throw new RuntimeException("There is no active session");
        }

        return currentSession().getUser();
    }

    public void destroyCurrentSession() {
        if(!hasActiveSession()) {
            throw new RuntimeException("There is no active session");
        }

        Http.Context ctx = Http.Context.current();

        InternalSession current = currentSession();
        current.delete();
        ctx.session().remove(CookieSessionName);
        ctx.args.remove(CtxCurrentSession);
    }

    public List<Session> sessionsByUser(User user) {
        List<InternalSession> sessions = InternalSession.finder.query().where().eq("user", user).findList();
        List<Session> result = new ArrayList<>();

        for(InternalSession s : sessions) {
            result.add(new Session(s));
        }

        return result;
    }

    public Optional<Session> getUserSession(User user, UUID sessionKey) {
        Optional<InternalSession> session = InternalSession.finder.query().where().eq("user", user).eq("sessionKey", sessionKey).findOneOrEmpty();

        Optional<Session> ret = Optional.empty();
        if(session.isPresent()) {
            ret = Optional.of(new Session(session.get()));
        }

        return ret;
    }

    public boolean hasActiveSession() {
        return currentSession() != null;
    }

    public void garbageCollect() {
        int deletedSessions = InternalSession.finder.query().where()
            .lt("issuedAt", DateTime.now().minusHours(ConstraintValues.SESSION_TIMEOUT_HOURS))
            .delete();

        Logger.info("REWRITE/ Delete "+deletedSessions+" Sessions");
    }

    public int getSessionTimeoutHours() {
        return ConstraintValues.SESSION_TIMEOUT_HOURS;
    }
}
