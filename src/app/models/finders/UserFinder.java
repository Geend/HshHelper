package models.finders;

import io.ebean.Finder;
import models.User;

import java.util.List;
import java.util.Optional;

public class UserFinder extends Finder<Long, User> {

    public UserFinder() {
        super(User.class);
    }

    public Optional<User> byIdOptional(Long id) {
        User u = this.byId(id);
        if (u == null) {
            return Optional.empty();
        }
        return Optional.of(u);
    }

    public List<User> findAllButThis(Long userId) {
        return this.query().where().notIn("userId", userId).findList();
    }

    public Optional<User> byName(String username) {
        return this.query().where().eq("username", username).findOneOrEmpty();
    }

    public Optional<User> byEmail(String email, UserFinderQueryOptions queryOptions) {
        switch(queryOptions) {
            case CaseInsensitive:
                return this.query().where().ieq("email", email).findOneOrEmpty();
            default:
                return this.query().where().eq("email", email).findOneOrEmpty();
        }
    }
}