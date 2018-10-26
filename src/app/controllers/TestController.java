package controllers;

import extension.AuthenticationRequired;
import extension.ContextArguments;
import models.User;
import play.mvc.Controller;
import play.mvc.Result;
import policy.session.Authorization;

import javax.inject.Singleton;
import java.util.Optional;

import static play.libs.Scala.asScala;

@Singleton
@Authorization.Required
public class TestController extends Controller {

    public Result index() {
        return ok("Got it!");
    }

}