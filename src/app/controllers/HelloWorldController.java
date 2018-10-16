package controllers;

import extension.AuthenticationRequired;
import extension.ContextArguments;

import models.User;
import models.UserSession;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Singleton;
import java.util.Optional;

@Singleton
@AuthenticationRequired
public class HelloWorldController extends Controller {

    public Result index() {
        return ok("Hello World");
    }

    public Result julius() {
        Optional<UserSession> userSession = ContextArguments.getUserSession();
        Optional<User> user = ContextArguments.getUser();

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
    public Result authrequired() {
        return ok("top secret");
    }
}
