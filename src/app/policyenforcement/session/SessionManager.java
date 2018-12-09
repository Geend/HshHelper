package policyenforcement.session;

import extension.B64Helper;
import extension.crypto.*;
import extension.RandomDataGenerator;
import extension.logging.DangerousCharFilteringLogger;
import io.ebean.Ebean;
import io.ebean.Update;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import models.User;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import play.Logger;
import play.mvc.Http;
import policyenforcement.ConstraintValues;
import policyenforcement.Policy;
import policyenforcement.session.exceptions.NoUserWithActiveSessionException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class SessionManager {
    private static final String CtxCurrentSession = "CurrentSession";
    private static final String CookieSessionName = "HsHSession";
    private static final String CookieSessionSecretName = "SessionSecret";

    private final KeyGenerator keyGenerator;
    private final Cipher cipher;
    private final RandomDataGenerator randomDataGenerator;
    private final B64Helper b64Helper;

    private static final Logger.ALogger logger = new DangerousCharFilteringLogger(
            SessionManager.class);

    @Inject
    public SessionManager(KeyGenerator keyGenerator, Cipher cipher, RandomDataGenerator randomDataGenerator, B64Helper b64Helper){
        this.keyGenerator = keyGenerator;
        this.cipher = cipher;
        this.randomDataGenerator = randomDataGenerator;
        this.b64Helper = b64Helper;
    }

    public void startNewSession(User user, byte[] credentialKeyPlaintext) {
        Http.Context ctx = Http.Context.current();

        byte[] credentialSecret = randomDataGenerator.generateBytes(CryptoConstants.GENERATED_KEY_BYTE);
        CryptoKey key = keyGenerator.generate(credentialSecret);
        CryptoResult encryptedCredentialKey = cipher.encrypt(key, credentialKeyPlaintext);

        InternalSession dbs = new InternalSession();
        // Kryptografisch sicher, siehe: https://docs.oracle.com/javase/7/docs/api/java/util/UUID.html#randomUUID()
        dbs.setSessionKey(UUID.randomUUID());
        dbs.setRemoteAddress(ctx.request().remoteAddress());
        dbs.setIssuedAt(DateTime.now());
        dbs.setUser(user);
        dbs.setInitializationVectorCredentialKey(encryptedCredentialKey.getInitializationVector());
        dbs.setCredentialKeyCipherText(encryptedCredentialKey.getCiphertext());
        dbs.save();

        ctx.session().clear();
        ctx.session().put(CookieSessionName, dbs.getSessionKey().toString());
        ctx.session().put(CookieSessionSecretName, b64Helper.encode(credentialSecret));
        ctx.args.remove(CtxCurrentSession);
    }

    private boolean isSessionTimedOut(InternalSession session) {
        return !session.getIssuedAt().plusMinutes(session.getUser().getSessionTimeoutInMinutes()).isAfterNow();
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
                        !isSessionTimedOut(dbs)) {
                    session = dbs;
                }
            }
        }

        ctx.args.put(CtxCurrentSession, session);
        return session;
    }

    public Policy currentPolicy() {
        return Policy.ForUser(currentUser());
    }

    public User currentUser() {
        if(!hasActiveSession()) {
            throw new NoUserWithActiveSessionException("There is no active session");
        }

        return currentSession().getUser();
    }

    public void destroySession(UUID sessionId) throws InvalidArgumentException, UnauthorizedException {
        Optional<Session> session = getUserSession(sessionId);
        if (!session.isPresent()) {
            throw new InvalidArgumentException("Diese Session existiert nicht.");
        }

        if (!currentPolicy().canDeleteSession(session.get())) {
            throw new UnauthorizedException("Du bist nicht authorisiert, diese Session zu zerstören.");
        }

        session.get().destroy();
    }

    private Optional<Session> getUserSession(UUID sessionKey) {
        Optional<InternalSession> session = InternalSession.finder.query().where().eq("sessionKey", sessionKey).findOneOrEmpty();

        Optional<Session> ret = Optional.empty();
        if(session.isPresent()) {
            ret = Optional.of(new Session(session.get()));
        }

        return ret;
    }

    public void destroyAllUserSessions(User user) {
        Update<InternalSession> upd = Ebean.createUpdate(InternalSession.class, "DELETE FROM InternalSession WHERE user=:user");
        upd.set("user", user.userId);
        upd.execute();
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

    public List<Session> activeSessionsByUser(User user) {
        List<InternalSession> sessions = InternalSession.finder.query().where().eq("user", user).findList();
        List<Session> result = new ArrayList<>();

        for(InternalSession s : sessions) {
            if(!isSessionTimedOut(s)) {
                result.add(new Session(s));
            }
        }

        return result;
    }

    public boolean hasActiveSession() {
        return currentSession() != null;
    }

    public void garbageCollect() {
        int deletedSessions = InternalSession.finder.query().where()
            .lt("issuedAt", DateTime.now().minusHours(ConstraintValues.MAX_SESSION_TIMEOUT_HOURS))
            .delete();

        logger.info("REWRITE/ Delete "+deletedSessions+" Sessions");
    }

    public int remainingSessionTime(){
        Minutes timeDifference = Minutes.minutesBetween(currentSession().getIssuedAt(), DateTime.now());
        return currentUser().getSessionTimeoutInMinutes() - timeDifference.getMinutes();
    }

    public byte[] getCredentialKey() {
        Http.Context ctx = Http.Context.current();
        if(!ctx.session().containsKey(CookieSessionSecretName)) {
            throw new RuntimeException("Ein Geheimnis ist benötigt!");
        }

        byte[] sessionSecret = b64Helper.decode(ctx.session().get(CookieSessionSecretName));
        CryptoKey key = keyGenerator.generate(sessionSecret);

        InternalSession session = currentSession();

        byte[] credentialKey = cipher.decrypt(key, session.getInitializationVectorCredentialKey(), session.getCredentialKeyCipherText());
        return credentialKey;
    }
}
