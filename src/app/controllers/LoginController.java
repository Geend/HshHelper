package controllers;

import models.User;
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

    public Result loginUnauthorized()
    {
        return unauthorized("ungültiges passwort du lümmel");
    }

    public Result loginCommit()
    {
        Form<UserLoginDto> boundForm = this.loginForm.bindFromRequest("userName", "password");
        if(boundForm.hasErrors()) {
            return redirect(routes.LoginController.loginUnauthorized());
        }
        UserLoginDto loginData = boundForm.get();

        if(User.authenticate(loginData.getUserName(), loginData.getPassword())) {
            return redirect(routes.HelloWorldController.index());
        }
        return redirect(routes.LoginController.loginUnauthorized());
    }
}
