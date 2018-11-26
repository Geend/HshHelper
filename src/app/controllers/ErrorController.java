package controllers;

import extension.ErrorHandler;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

public class ErrorController  extends Controller {

    public Result showForbiddenMessage() {
        String message = Http.Context.current().flash().get(ErrorHandler.ERROR_KEY);

        return forbidden(views.html.error.Forbidden.render(message));
    }

    public Result showBadRequestMessage() {
        String message = Http.Context.current().flash().get(ErrorHandler.ERROR_KEY);

        return badRequest(views.html.error.BadRequest.render(message));
    }
}
