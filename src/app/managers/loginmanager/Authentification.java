package managers.loginmanager;

import extension.HashHelper;
import models.User;
import models.finders.UserFinder;

import javax.inject.Inject;
import java.util.Optional;

public class Authentification {
    private static UserFinder UserFinder = new UserFinder();

    public static class Result {
        private boolean success;
        private boolean userExists;
        private User user;

        public Result(boolean success, boolean userExists, User user) {
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

    private UserFinder userFinder;
    private HashHelper hashHelper;

    @Inject
    public Authentification(UserFinder userFinder, HashHelper hashHelper) {
        this.userFinder = userFinder;
        this.hashHelper = hashHelper;
    }

    public Result Perform(String username, String password) {
        Optional<User> user = userFinder.byName(username);

        // Nutzer existiert nicht!
        if(!user.isPresent()) {
            // Statistik-Angriff verhindern.
            // Aufrufzeit darf sich nicht maßgeblich unterscheiden, wenn kein Nutzer existiert.
            hashHelper.hashPassword(password);
            return new Result(false, false, null);
        }

        // Nutzer existiert
        boolean success = hashHelper.checkHash(password, user.get().getPasswordHash());
        return new Result(success, true, user.get());
    }
}
