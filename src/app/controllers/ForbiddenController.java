package controllers;

import models.User;
import play.mvc.Controller;
import play.mvc.Result;

import static play.libs.Scala.asScala;

public class ForbiddenController  extends Controller {

    public Result showForbiddenMessage() {
        return ok(views.html.Forbidden.render());
    }
}
