package extension;

import models.User;
import org.joda.time.DateTime;
import org.joda.time.Minutes;

public class AuthenticateUser {

    public boolean SecureAuthenticate(User user, String password)
    {
        Minutes minutesSinceLastAttempt = Minutes.minutes(0);
        if(user.getMostRecentLoginAttempt() != null) {
            minutesSinceLastAttempt = Minutes.minutesBetween(user.getMostRecentLoginAttempt(), DateTime.now());
        }

        if(user.getInvalidLoginCounter() == 5 ) {
            if(minutesSinceLastAttempt.getMinutes() >= 1) {
                user.setInvalidLoginCounter(0);
                user.save();
            }
            else {
                return false;
            }
        }

        boolean passwordMatch = HashHelper.checkHash(password, user.getPasswordHash());
        if(!passwordMatch) {
            user.setInvalidLoginCounter(user.getInvalidLoginCounter() + 1);
        }
        else {
            user.setInvalidLoginCounter(0);
        }
        user.setMostRecentLoginAttempt(DateTime.now());
        user.save();
        return passwordMatch;
    }
}
