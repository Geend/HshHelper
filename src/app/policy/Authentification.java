package policy;

import models.finders.UserFinder;

public class Authentification {
    private static UserFinder UserFinder = new UserFinder();

    public class Result {
        private boolean userExists;
        private boolean success;

        private Result(boolean userExists, boolean success) {
            this.userExists = userExists;
            this.success = success;
        }

        public boolean userExists() {
            return this.userExists;
        }

        public boolean success() {
            return this.success;
        }
    }

    public static void Perform(String username, String password) {

    }
}
