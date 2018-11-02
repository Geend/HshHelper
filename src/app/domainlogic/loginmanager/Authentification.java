package domainlogic.loginmanager;

import extension.HashHelper;
import models.User;
import models.finders.UserFinder;

import java.util.Optional;

public class Authentification {
    private static UserFinder UserFinder = new UserFinder();

    public static class Result {
        private boolean success;
        private boolean userExists;
        private User user;

        private Result(boolean success, boolean userExists, User user) {
            this.success = success;
            this.userExists = userExists;
            this.user = user;
        }

        public boolean userExists() {
            return this.userExists;
        }
        public boolean success() {
            return this.success;
        }
        public User user() {
            return this.user;
        }
    }

    public static Result Perform(String username, String password) {
        Optional<User> user = UserFinder.byName(username);

        // Nutzer existiert nicht!
        if(!user.isPresent()) {
            return new Result(false, false, null);
        }

        // Nutzer existiert
        boolean success = HashHelper.checkHash(password, user.get().getPasswordHash());
        return new Result(success, true, user.get());
    }
}
