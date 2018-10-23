package models;

import javax.persistence.*;

import org.joda.time.DateTime;
import play.data.validation.Constraints;

import java.util.*;

@Entity
@Table(name = "users")
public class User extends BaseDomain {

    @Column(unique = true)
    public String username;
    @Constraints.Email
    public String email;
    public String passwordHash;
    public boolean passwordResetRequired;
    public int quotaLimit;

    public DateTime mostRecentLoginAttempt;
    public int invalidLoginCounter;

    @ManyToMany(cascade = {
            CascadeType.REMOVE,
            CascadeType.MERGE
    })
    @JoinTable(name = "groupmembers",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    public Set<Group> groups = new HashSet<>();

    public User(
            String username,
            String email,
            String passwordHash,
            boolean passwordResetRequired,
            int quotaLimit) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.passwordResetRequired = passwordResetRequired;
        this.quotaLimit = quotaLimit;
    }

    public boolean isAdmin() {
        return this.groups.stream().anyMatch(x -> x.isAdminGroup);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        // A situation could occur where users have not been added to the DB yet but still need
        // to be comparable. During a test, for example.
        if (user.id == null) {
            return Objects.equals(username, user.username);
        }
        return Objects.equals(id, user.id) &&
                Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }
}
