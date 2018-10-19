package extension;

import models.User;
import org.joda.time.DateTime;
import org.joda.time.Minutes;

public class AuthenticateUser {

    public boolean SecureAuthenticate(User user, String password)
    {
        Minutes minutesSinceLastAttempt = Minutes.minutes(0);
        if(user.mostRecentLoginAttempt != null) {
            minutesSinceLastAttempt = Minutes.minutesBetween(user.mostRecentLoginAttempt, DateTime.now());
        }

        if(user.invalidLoginCounter == 5 ) {
            if(minutesSinceLastAttempt.getMinutes() >= 1) {
                user.invalidLoginCounter = 0;
                user.save();
            }
            else {
                return false;
            }
        }

        boolean passwordMatch = HashHelper.checkHash(password, user.passwordHash);
        if(!passwordMatch) {
            user.invalidLoginCounter++;
        }
        else {
            user.invalidLoginCounter = 0;
        }
        user.mostRecentLoginAttempt = DateTime.now();
        user.save();
        return passwordMatch;
    }
}
