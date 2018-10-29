package policy.session;

import models.User;
import org.joda.time.DateTime;

import java.util.UUID;

public class UserSession {
    private Session session;

    protected UserSession(Session underlyingSession) {
        this.session = underlyingSession;
    }

    public DateTime getIssuedAt() {
        return session.getIssuedAt();
    }

    public String getRemoteAddress() {
        return session.getRemoteAddress();
    }

    public User getUser() {
        return session.getUser();
    }

    public UUID getSessionKey() {
        return session.getSessionKey();
    }

    public void destroy() {
        session.delete();
    }
}
