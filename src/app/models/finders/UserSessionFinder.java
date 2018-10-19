package models.finders;

import io.ebean.Finder;
import models.UserSession;

import java.util.Optional;

public class UserSessionFinder extends Finder<Long, UserSession> {

    public UserSessionFinder() {
        super(UserSession.class);
    }

    public Optional<UserSession> byIdOptional(Long id) {
        return Optional.of(this.byId(id));
    }
}
