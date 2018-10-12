package controllers;

import models.dtos.UserLoginDto;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;

public class LoginController extends Controller {

    private Form<UserLoginDto> loginForm;

    @Inject
    public LoginController(FormFactory formFactory) {
        loginForm = formFactory.form(UserLoginDto.class);
    }

    public Result login() {
        return ok(views.html.Login.render(loginForm));
    }

    public Result loginCommit()
    {
        return ok();
    }
}
