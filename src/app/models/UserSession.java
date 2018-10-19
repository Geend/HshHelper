package models;

import org.joda.time.DateTime;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "usersession")
public class UserSession extends BaseDomain {

    private Long userId;
    private DateTime issuedAt;
    private String connectedFrom;

    public String getConnectedFrom() {
        return connectedFrom;
    }

    public void setConnectedFrom(String connectedFrom) {
        this.connectedFrom = connectedFrom;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public DateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(DateTime issuedAt) {
        this.issuedAt = issuedAt;
    }
}
