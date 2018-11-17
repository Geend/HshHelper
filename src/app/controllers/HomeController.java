package controllers;

import models.User;
import play.mvc.Controller;
import play.mvc.Result;
import policyenforcement.session.Authentication;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import static play.libs.Scala.asScala;

@Singleton
@Authentication.Required
public class HomeController extends Controller {
    private final SessionManager sessionManager;

    @Inject
    public HomeController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public Result index() {
        User user = sessionManager.currentUser();
        return ok(views.html.Index.render(user, asScala(user.getGroups())));
    }

}