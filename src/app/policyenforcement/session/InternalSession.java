package policyenforcement.session;

import io.ebean.Finder;
import io.ebean.Model;
import models.User;
import org.joda.time.DateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.UUID;

@Entity
public class InternalSession extends Model {
    // TODO: Herausfinden ob garantiert werden kann, dass UUID.randomUUID() verwendet wird
    // -> nutzt SecureRandom!
    @Id
    private UUID sessionKey;
    private String remoteAddress;
    private byte[] initializationVectorCredentialKey;
    private byte[] credentialKeyCipherText;
    private DateTime issuedAt;

    protected InternalSession() {}

    @ManyToOne
    @JoinColumn(name = "user", referencedColumnName = "user_id")
    private User user;

    public UUID getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(UUID sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public DateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(DateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public byte[] getInitializationVectorCredentialKey() {
        return initializationVectorCredentialKey;
    }

    public void setInitializationVectorCredentialKey(byte[] initializationVectorCredentialKey) {
        this.initializationVectorCredentialKey = initializationVectorCredentialKey;
    }

    public byte[] getCredentialKeyCipherText() {
        return credentialKeyCipherText;
    }

    public void setCredentialKeyCipherText(byte[] credentialKeyCipherText) {
        this.credentialKeyCipherText = credentialKeyCipherText;
    }

    protected static Finder<UUID, InternalSession> finder = new Finder<>(InternalSession.class);
}
