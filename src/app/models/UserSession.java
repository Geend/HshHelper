package models;

import org.joda.time.DateTime;

import javax.persistence.*;

@Entity
@Table(name = "usersession")
public class UserSession extends BaseDomain {

    private DateTime issuedAt;
    private String connectedFrom;
    @ManyToOne
    @JoinColumn(name = "user", referencedColumnName = "id")
    private User user;

    public String getConnectedFrom() {
        return connectedFrom;
    }

    public void setConnectedFrom(String connectedFrom) {
        this.connectedFrom = connectedFrom;
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
}
