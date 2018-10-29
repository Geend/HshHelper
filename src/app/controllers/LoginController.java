package controllers;

import extension.HashHelper;
import extension.RecaptchaHelper;
import models.User;
import models.dtos.ChangePasswordAfterResetDto;
import models.dtos.UserLoginDto;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;
import policy.Authentification;
import policy.ext.loginFirewall.Firewall;
import policy.ext.loginFirewall.Instance;
import policy.ext.loginFirewall.Strategy;
import policy.session.Authentication;
import policy.session.SessionManager;


import javax.inject.Inject;

public class LoginController extends Controller {

    private Form<UserLoginDto> loginForm;
    private Form<ChangePasswordAfterResetDto> changePasswordForm;

    @Inject
    public LoginController(
            FormFactory formFactory) {
        this.loginForm = formFactory.form(UserLoginDto.class);
        this.changePasswordForm = formFactory.form(ChangePasswordAfterResetDto.class);
    }

    @Authentication.NotAllowed
    public Result showLoginForm() {
        return ok(views.html.Login.render(loginForm, false));
    }

    @Authentication.NotAllowed
    public Result login() {
        Form<UserLoginDto> boundForm = this.loginForm.bindFromRequest("username", "password", "recaptcha");
        if (boundForm.hasErrors()) {
            return redirect(routes.LoginController.login());
        }

        UserLoginDto loginData = boundForm.get();

        Authentification.Result auth = Authentification.Perform(
                loginData.getUsername(),
                loginData.getPassword()
        );

        Long uid = null;
        if(auth.userExists()) {
            uid = auth.user().getUserId();
        }

        Instance fw = Firewall.Get(request().remoteAddress());
        Strategy strategy = fw.getStrategy(uid);

        if(strategy.equals(Strategy.BLOCK)) {
            boundForm = boundForm.withGlobalError("You're banned from logging in");
            return badRequest(views.html.Login.render(boundForm, false));
        }

        if(strategy.equals(Strategy.VERIFY)) {
            if(!RecaptchaHelper.IsValidResponse(loginData.getRecaptcha(), request().remoteAddress())) {
                boundForm = boundForm.withGlobalError("Complete the Captcha!");
                return badRequest(views.html.Login.render(boundForm, true));
            }
        }

        // Wenn der Login nicht erfolgreich war Request abbrechen und Nutzer informieren!
        if(!auth.success()) {
            boundForm = boundForm.withGlobalError("Invalid Login Data!");
            fw.fail(uid);
            return badRequest(views.html.Login.render(boundForm, strategy.equals(Strategy.VERIFY)));
        }

        // TODO: Ugly AF / muss weg. Ist Policy-Frage und sollte nicht Gegenstand von Hacky-Code sein!
        if(auth.user().getIsPasswordResetRequired()) {
            return redirect(routes.LoginController.changePasswordAfterReset());
        }

        SessionManager.StartNewSession(auth.user());

        return redirect(routes.HomeController.index());
    }

    @Authentication.NotAllowed
    public Result showChangePasswordAfterResetForm() {
        return ok(views.html.ChangePasswordAfterReset.render(this.changePasswordForm, false));
    }

    @Authentication.NotAllowed
    public Result changePasswordAfterReset() {
        Form<ChangePasswordAfterResetDto> boundForm = this.changePasswordForm.bindFromRequest("username", "currentPassword", "password", "passwordRepeat", "recaptcha");
        if (boundForm.hasErrors()) {
            return ok(views.html.ChangePasswordAfterReset.render(boundForm, false));
        }

        ChangePasswordAfterResetDto changePasswordData = boundForm.get();

        Authentification.Result auth = Authentification.Perform(
                changePasswordData.getUsername(),
                changePasswordData.getCurrentPassword()
        );

        Long uid = null;
        if(auth.userExists()) {
            uid = auth.user().getUserId();
        }

        Instance fw = Firewall.Get(request().remoteAddress());
        Strategy strategy = fw.getStrategy(uid);

        if(strategy.equals(Strategy.BLOCK)) {
            boundForm = boundForm.withGlobalError("You're banned from logging in");
            return badRequest(views.html.ChangePasswordAfterReset.render(boundForm, false));
        }

        if(strategy.equals(Strategy.VERIFY)) {
            if(!RecaptchaHelper.IsValidResponse(changePasswordData.getRecaptcha(), request().remoteAddress())) {
                boundForm = boundForm.withGlobalError("Complete the Captcha!");
                return badRequest(views.html.ChangePasswordAfterReset.render(boundForm, true));
            }
        }

        // Wenn der Login nicht erfolgreich war Request abbrechen und Nutzer informieren!
        if(!auth.success()) {
            boundForm = boundForm.withGlobalError("Invalid Login Data!");
            fw.fail(uid);
            return badRequest(views.html.ChangePasswordAfterReset.render(boundForm, strategy.equals(Strategy.VERIFY)));
        }

        User user = auth.user();
        user.setIsPasswordResetRequired(false);
        user.setPasswordHash(HashHelper.hashPassword(changePasswordData.getPassword()));
        user.save();

        return redirect(routes.LoginController.login());
    }

    @Authentication.Required
    public Result logout() {
        SessionManager.DestroyCurrentSession();
        return redirect(routes.HomeController.index());
    }
}
