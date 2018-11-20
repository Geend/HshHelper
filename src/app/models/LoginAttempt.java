package models;

import io.ebean.Model;
import org.joda.time.DateTime;

import javax.persistence.*;

@Entity
@Table(name = "login_attempt")
public class LoginAttempt extends Model {
    @Id
    private Long loginAttemptId;

    private String address;

    private String clientName;

    private DateTime dateTime;

    @ManyToOne
    @JoinColumn(name = "fk_user_id", referencedColumnName = "user_id")
    private User user;


    public Long getLoginAttemptId() {
        return loginAttemptId;
    }

    public void setLoginAttemptId(Long loginAttemptId) {
        this.loginAttemptId = loginAttemptId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }
}
