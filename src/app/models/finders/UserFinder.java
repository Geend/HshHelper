package models.finders;

import extension.HashHelper;
import io.ebean.Finder;
import models.User;

import java.util.Optional;

public class UserFinder extends Finder<Long, User> {

    public UserFinder() {
        super(User.class);
    }

    public Optional<User> byIdOptional(Long id) {
        return Optional.of(this.byId(id));
    }

    public Optional<User> byName(String username) {
        return this.query().where().eq("username", username).findOneOrEmpty();
    }

    public Optional<User> authenticate(String username, String password) {
        Optional<User> user = this.query().where()
                .eq("username", username)
                .findOneOrEmpty();

        if(!user.isPresent())
            return user;
        else if(HashHelper.checkHash(password, user.get().passwordHash))
            return user;
        else
            return Optional.empty();
    }
}