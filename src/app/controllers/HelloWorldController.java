package controllers;

import extension.UserSessionProvider;
import models.UserSession;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HelloWorldController extends Controller {

    public Result index() {
        return ok("Hello World");
    }

    @With(UserSessionProvider.class)
    public Result julius() {
        UserSession session = (UserSession)ctx().args.get("SessionObject");
        if(session != null) {
            return ok("authenticated");
        }
        else {
            return ok("not authenticated");
        }
    }
}
