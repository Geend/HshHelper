package models.finders;

import io.ebean.Finder;
import models.User;
import models.UserSession;

import java.util.List;
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

    public List<UserSession> byUser(User user) {
        return this.query().where().eq("user", user).findList();
    }
}
