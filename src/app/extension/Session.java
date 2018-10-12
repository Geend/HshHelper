package extension;

import models.User;
import play.mvc.Http;

public class Session {
    public static boolean IsAdmin() {
        return true;
    }

    public static User GetUser() {
        //TODO:  Http.Context.current().args.get("user...")
        return User.findAll().get(0);
    }
}
