package policy.session;

import org.joda.time.DateTime;

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

    public void destroy() {
        session.delete();
    }
}
