package models.finders;

import extension.HashHelper;
import io.ebean.Finder;
import models.User;

import java.util.List;
import java.util.Optional;

public class UserFinder extends Finder<Long, User> {

    /**
     * Construct using the default EbeanServer.
     */
    public UserFinder() {
        super(User.class);
    }

    public Optional<User> byIdOptional(Long id) {
        return Optional.of(this.byId(id));
    }

    public Optional<User> byName(String username) {
        return this.query().where().eq("username", username).findOneOrEmpty();
    }

    public boolean authenticate(String username, String password) {
        Optional<User> user = this.query().where()
                .eq("username", username)
                .findOneOrEmpty();

        if(!user.isPresent())
            return false;

        return HashHelper.checkHash(password, user.get().passwordHash);
    }
}