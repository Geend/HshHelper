package models.finders;

import io.ebean.Finder;
import models.LoginAttempt;

public class LoginAttemptFinder extends Finder<Long, LoginAttempt> {
    public LoginAttemptFinder() {
        super(LoginAttempt.class);
    }
}
