package models;

import javax.persistence.*;

import io.ebean.Model;
import models.finders.UserFinder;
import play.data.validation.Constraints;

import java.util.*;

@Entity
@Table(name = "users")
public class User extends Model {

    @Id
    //@GeneratedValue(strategy=GenerationType.AUTO)
    public Long id;
    @Column(unique = true)
    public String username;
    @Constraints.Email
    public String email;
    public String passwordHash;
    public boolean passwordResetRequired;
    public int quotaLimit;

    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(name = "groupmembers",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    public Set<Group> groups = new HashSet<>();

    public static final UserFinder find = new UserFinder();

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


    public static List<User> findAll() {
        return find.all();
    }

    public static Optional<User> findById(Long id) {
        return Optional.of(find.byId(id));
    }

    public static Optional<User> findByName(String username){
        return find.query().where().eq("username", username).findOneOrEmpty();
    }

    public static boolean authenticate(String username, String password) {
        return find.query().where()
                .eq("username", username)
                .and()
                .eq("password_hash", password)
                .findOneOrEmpty().isPresent();
    }

    public boolean isAdmin() {
        return this.groups.stream().anyMatch(x -> x.isAdminGroup);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return passwordResetRequired == user.passwordResetRequired &&
                quotaLimit == user.quotaLimit &&
                Objects.equals(id, user.id) &&
                Objects.equals(username, user.username) &&
                Objects.equals(email, user.email) &&
                Objects.equals(passwordHash, user.passwordHash) &&
                Objects.equals(groups, user.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email, passwordHash, passwordResetRequired, quotaLimit, groups);
    }
}
