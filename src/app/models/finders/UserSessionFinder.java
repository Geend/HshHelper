package models.finders;

import io.ebean.Finder;
import models.UserSession;

import java.util.Optional;

public class UserSessionFinder extends Finder<Long, UserSession> {

    public UserSessionFinder() {
        super(UserSession.class);
    }

    public Optional<UserSession> byIdOptional(Long id) {
        UserSession userSession = this.byId(id);
        if(userSession == null) {
            return Optional.empty();
        }
        return Optional.of(userSession);
    }
}
