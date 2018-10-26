package extension;

import models.User;
import models.UserSession;
import play.mvc.Http;

import java.util.Optional;

public class ContextArgumentsHelper {
    private static final String userSessionKey = "userSessionObject";
    private static final String userKey = "userObject";

    public Optional<UserSession> getUserSession()  {
        Http.Context context = Http.Context.current();
        if(context.args.containsKey(userSessionKey)) {
            return Optional.of((UserSession) context.args.get(userSessionKey));
        }
        else {
            return Optional.empty();
        }
    }

    public void setUserSession(UserSession value) {
        Http.Context context = Http.Context.current();
        context.args.put(userSessionKey, value);
    }

    public Optional<User> getUser()  {
        Http.Context context = Http.Context.current();
        if(context.args.containsKey(userKey)) {
            return Optional.of((User) context.args.get(userKey));
        }
        else {
            return Optional.empty();
        }
    }

    public void setUser(User value) {
        Http.Context context = Http.Context.current();
        context.args.put(userKey, value);
    }
}
