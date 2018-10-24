package controllers;

import extension.AuthenticationRequired;
import extension.ContextArguments;

import models.User;
import models.UserSession;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Singleton;
import java.util.Optional;

import static play.libs.Scala.asScala;

@Singleton
@AuthenticationRequired
public class HomeController extends Controller {

    public Result index() {
        Optional<User> user = extension.ContextArguments.getUser();
        if (user.isPresent()) {
            return ok(views.html.Index.render(user.get(), asScala(user.get().getGroups())));
        }
        return badRequest("error");
    }

}