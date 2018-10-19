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
public class HomeController extends Controller {

    public Result index() {
        Optional<User> user = extension.ContextArguments.getUser();
        if (user.isPresent()) {
            return ok(views.html.Index.render(user.get()));
        }
        return badRequest("error");
    }

}