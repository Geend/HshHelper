package controllers;

import models.User;
import play.mvc.Controller;
import play.mvc.Result;
import policy.session.Authentication;
import policy.session.SessionManager;

import javax.inject.Singleton;

import static play.libs.Scala.asScala;

@Singleton
@Authentication.Required
public class HomeController extends Controller {

    public Result index() {
        User user = SessionManager.CurrentUser();
        return ok(views.html.Index.render(user, asScala(user.getGroups())));
    }

}