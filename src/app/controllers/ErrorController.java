package controllers;

import extension.ErrorHandler;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

public class ErrorController  extends Controller {

    public Result showForbiddenMessage() {
        return forbidden(views.html.error.Forbidden.render(null));
    }

    public Result showBadRequestMessage() {
        return badRequest(views.html.error.BadRequest.render(null));
    }
}
