package controllers;

import extension.AuthenticationRequired;
import extension.ContextArguments;
import extension.UserProvider;
import extension.UserSessionProvider;
import models.User;
import models.UserSession;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class HelloWorldController extends Controller {

    public Result index() {
        return ok("Hello World");
    }

    @With({UserSessionProvider.class, UserProvider.class})
    public Result julius() {
        Optional<UserSession> userSession = ContextArguments.getUserSession(ctx());
        Optional<User> user = ContextArguments.getUser(ctx());

        if(userSession.isPresent()) {
            if(user.isPresent()) {
                return ok("authenticated user: test");
            }
            return ok("authenticated");
        }
        else {
            return ok("not authenticated");
        }
    }
    @With({UserSessionProvider.class, UserProvider.class, AuthenticationRequired.class})
    public Result authrequired() {
        return ok("top secret");
    }
}
