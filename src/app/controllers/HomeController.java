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
        return ok("Hello World");
    }

}