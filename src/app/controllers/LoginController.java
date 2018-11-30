package controllers;

import managers.loginmanager.*;
import dtos.ChangePasswordAfterResetDto;
import dtos.UserLoginDto;
import play.data.Form;
import play.data.FormFactory;
import play.filters.csrf.CSRF;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import policyenforcement.session.Authentication;


import javax.inject.Inject;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

import static extension.StringHelper.empty;

public class LoginController extends Controller {

    private final Form<UserLoginDto> loginForm;
    private final Form<ChangePasswordAfterResetDto> changePasswordForm;
    private final LoginManager loginManager;

    @Inject
    public LoginController(
            FormFactory formFactory, LoginManager loginManager) {
        this.loginForm = formFactory.form(UserLoginDto.class);
        this.changePasswordForm = formFactory.form(ChangePasswordAfterResetDto.class);
        this.loginManager = loginManager;
    }

    // Not allowed wichtig, kann sonst zum DOS verwendet werden!
    @Authentication.NotAllowed
    public Result showLoginForm() {
        return ok(views.html.login.Login.render(loginForm, false));
    }

    @Authentication.NotAllowed
    public Result login() throws IOException {
        Form<UserLoginDto> boundForm = this.loginForm.bindFromRequest("username", "password", "twofactorpin");
        if (boundForm.hasErrors()) {
            return badRequest(views.html.login.Login.render(boundForm, false));
        }

        Optional<CSRF.Token> token = CSRF.getToken(request());
        if(!token.isPresent()) {
            // bewusst nicht auf bad request sondern wieder auf login da bad request
            // einen angemeldenten benutzer erwartet
            return redirect(routes.LoginController.login());
        }

        UserLoginDto loginData = boundForm.get();
        Optional<String> recaptchaData = boundForm.field("g-recaptcha-response").getValue();
        if(recaptchaData.isPresent()) {
            loginData.setRecaptcha(recaptchaData.get());
        }

        Integer twoFactorPin = 0;
        if(!empty(loginData.getTwofactorpin())) {
            twoFactorPin = Integer.parseInt(loginData.getTwofactorpin());
        }

        try {
            loginManager.login(
                    loginData.getUsername(),
                    loginData.getPassword(),
                    loginData.getRecaptcha(),
                    Http.Context.current().request(),
                    twoFactorPin);
        } catch (CaptchaRequiredException e) {
            boundForm = boundForm.withGlobalError("Complete the Captcha!");
            return badRequest(views.html.login.Login.render(boundForm, true));
        } catch (InvalidLoginException e) {
            boundForm = boundForm.withGlobalError("Invalid Login Data!");
            return badRequest(views.html.login.Login.render(boundForm, false));
        } catch (PasswordChangeRequiredException e) {
            return redirect(routes.LoginController.changePasswordAfterReset());
        } catch (GeneralSecurityException e) {
            return badRequest(views.html.login.Login.render(boundForm, false));
        }

        return redirect(routes.HomeController.index());
    }

    // NotAllowed wichtig, kann sonst für DOS verwendet werden!
    @Authentication.NotAllowed
    public Result showChangePasswordAfterResetForm() {
        return ok(views.html.login.ChangePasswordAfterReset.render(this.changePasswordForm, false));
    }

    // TODO: Sollte man nach password reset eingelogged sein?!
    @Authentication.NotAllowed
    public Result changePasswordAfterReset() throws IOException {
        Form<ChangePasswordAfterResetDto> boundForm = this.changePasswordForm.bindFromRequest("username", "currentPassword", "password", "passwordRepeat");
        if (boundForm.hasErrors()) {
            return badRequest(views.html.login.ChangePasswordAfterReset.render(boundForm, false));
        }

        ChangePasswordAfterResetDto changePasswordData = boundForm.get();
        Optional<String> recaptchaData = boundForm.field("g-recaptcha-response").getValue();
        if(recaptchaData.isPresent()) {
            changePasswordData.setRecaptcha(recaptchaData.get());
        }

        try {
            loginManager.changePassword(
                    changePasswordData.getUsername(),
                    changePasswordData.getCurrentPassword(),
                    changePasswordData.getPassword(),
                    changePasswordData.getRecaptcha(),
                    Http.Context.current().request(),
                    0);
        } catch (InvalidLoginException e) {
            boundForm = boundForm.withGlobalError("Invalid Login Data!");
            return badRequest(views.html.login.ChangePasswordAfterReset.render(boundForm, false));
        } catch (CaptchaRequiredException e) {
            boundForm = boundForm.withGlobalError("Complete the Captcha!");
            return badRequest(views.html.login.ChangePasswordAfterReset.render(boundForm, true));
        } catch (GeneralSecurityException e) {
            return badRequest(views.html.login.ChangePasswordAfterReset.render(boundForm, false));
        }

        return redirect(routes.LoginController.login());
    }

    @Authentication.Required
    public Result logout() {
        loginManager.logout();
        return redirect(routes.LoginController.login());
    }
}
