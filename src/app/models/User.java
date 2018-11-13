package models;

import javax.persistence.*;

import io.ebean.Model;
import org.joda.time.DateTime;
import play.data.validation.Constraints;

import java.util.*;

@Entity
@Table(name = "users")
public class User extends Model {

    @Id
    public Long userId;

    @Column(unique = true)
    private String username;

    @Constraints.Email
    private String email;

    private String passwordHash;

    private boolean passwordResetRequired;

    private int quotaLimit;

    @OneToMany(
        mappedBy = "owner",
        cascade = CascadeType.ALL
    )
    private Set<Group> ownerOf = new HashSet<>();


    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.REMOVE,
            CascadeType.MERGE
    })
    @JoinTable(name = "groupmembers",
            joinColumns = @JoinColumn(name = "fk_user_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "fk_group_id", referencedColumnName = "group_id")
    )
    private Set<Group> groups = new HashSet<>();


    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.REMOVE,
            CascadeType.MERGE
    })
    @JoinTable(name = "user_permissions",
            joinColumns = @JoinColumn(name = "fk_user_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "fk_user_permission_id", referencedColumnName = "user_permission_id")
    )
    private Set<UserPermission> userPermissions = new HashSet<>();


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
        return this.groups.stream().anyMatch(x -> x.getIsAdminGroup());
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean getIsPasswordResetRequired() {
        return passwordResetRequired;
    }

    public void setIsPasswordResetRequired(boolean passwordResetRequired) {
        this.passwordResetRequired = passwordResetRequired;
    }

    public int getQuotaLimit() {
        return quotaLimit;
    }

    public void setQuotaLimit(int quotaLimit) {
        this.quotaLimit = quotaLimit;
    }

    public Set<Group> getOwnerOf() {
        return ownerOf;
    }

    public void setOwnerOf(Set<Group> ownerOf) {
        this.ownerOf = ownerOf;
    }

    public Set<Group> getGroups() {
        return groups;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    public Set<UserPermission> getUserPermissions() {
        return userPermissions;
    }

    public void setUserPermissions(Set<UserPermission> userPermissions) {
        this.userPermissions = userPermissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        // A situation could occur where users have not been added to the DB yet but still need
        // to be comparable. During a test, for example.
        if (user.userId == null) {
            return Objects.equals(username, user.username);
        }
        return Objects.equals(userId, user.userId) &&
                Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, username);
    }


}
