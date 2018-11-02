package policy.session;

import models.User;
import org.joda.time.DateTime;

import java.util.UUID;

public class Session {
    private InternalSession internalSession;

    protected Session(InternalSession underlyingInternalSession) {
        this.internalSession = underlyingInternalSession;
    }

    public DateTime getIssuedAt() {
        return internalSession.getIssuedAt();
    }

    public String getRemoteAddress() {
        return internalSession.getRemoteAddress();
    }

    public User getUser() {
        return internalSession.getUser();
    }

    public UUID getSessionKey() {
        return internalSession.getSessionKey();
    }

    public void destroy() {
        internalSession.delete();
    }
}
