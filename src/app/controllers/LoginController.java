package controllers;

import constants.CookieConstants;
import extension.AuthenticateUser;
import extension.HashHelper;
import models.User;
import models.UserSession;
import models.dtos.ChangePasswordAfterResetDto;
import models.dtos.UserLoginDto;
import models.finders.UserFinder;
import models.finders.UserSessionFinder;
import org.joda.time.DateTime;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.ChangePasswordAfterReset;


import javax.inject.Inject;
import java.util.Optional;

public class LoginController extends Controller {

    private Form<UserLoginDto> loginForm;
    private Form<ChangePasswordAfterResetDto> changePasswordForm;
    private UserSessionFinder userSessionFinder;
    private UserFinder userFinder;
    private AuthenticateUser authenticateUser;

    @Inject
    public LoginController(
            FormFactory formFactory,
            UserSessionFinder userSessionFinder,
            UserFinder userFinder,
            AuthenticateUser authenticateUser) {
        this.loginForm = formFactory.form(UserLoginDto.class);
        this.changePasswordForm = formFactory.form(ChangePasswordAfterResetDto.class);
        this.userSessionFinder = userSessionFinder;
        this.userFinder = userFinder;
        this.authenticateUser = authenticateUser;
    }

    public Result showLoginForm() {
        if (session().containsKey(CookieConstants.USER_SESSION_ID_NAME)) {
            return redirect(routes.HomeController.index());
        }
        return ok(views.html.Login.render(loginForm));
    }

    public Result login() {
        Form<UserLoginDto> boundForm = this.loginForm.bindFromRequest("username", "password");
        if (boundForm.hasErrors()) {
            return redirect(routes.LoginController.login());
        }
        UserLoginDto loginData = boundForm.get();

        Optional<User> user = userFinder.byName(loginData.getUsername());
        if(!user.isPresent()) {
            return redirect(routes.LoginController.login());
        }

        if(this.authenticateUser.SecureAuthenticate(user.get(), loginData.getPassword())) {
            if(user.get().passwordResetRequired) {
                return redirect(routes.LoginController.changePasswordAfterReset());
            }

            Logger.info("User authenticated");

            String remoteIp = request().remoteAddress();
            UserSession userSession = new UserSession();
            userSession.setConnectedFrom(remoteIp);
            userSession.setUser(user.get());
            userSession.setIssuedAt(DateTime.now());
            userSession.save();

            session().put(CookieConstants.USER_SESSION_ID_NAME, userSession.getId().toString());

            return redirect(routes.HomeController.index());
        }

        Logger.info("User could not be authenticated");
        return redirect(routes.LoginController.login());
    }

    public Result showChangePasswordAfterResetForm()
    {
        return ok(views.html.ChangePasswordAfterReset.render(this.changePasswordForm));
    }

    public Result changePasswordAfterReset()
    {
        Form<ChangePasswordAfterResetDto> boundForm = this.changePasswordForm.bindFromRequest("username", "currentPassword", "password", "passwordRepeat");
        if (boundForm.hasErrors()) {
            return ok(views.html.ChangePasswordAfterReset.render(boundForm));
        }
        ChangePasswordAfterResetDto changePasswordData = boundForm.get();
        Optional<User> userOptional = this.userFinder.byName(changePasswordData.getUsername());
        if(!userOptional.isPresent()) {
            return redirect(routes.LoginController.login());
        }

        User user = userOptional.get();
        if(this.authenticateUser.SecureAuthenticate(user, changePasswordData.getCurrentPassword())) {
            user.passwordResetRequired = false;
            user.passwordHash = HashHelper.hashPassword(changePasswordData.getPassword());
            user.save();
        }
        return redirect(routes.LoginController.login());
    }

    public Result logout() {

        if (session().containsKey(CookieConstants.USER_SESSION_ID_NAME)) {

            String sessionIdString = session().getOrDefault(CookieConstants.USER_SESSION_ID_NAME, null);
            session().remove(CookieConstants.USER_SESSION_ID_NAME);

            Long sessionId = Long.parseLong(sessionIdString);
            Optional<UserSession> session = this.userSessionFinder.byIdOptional(sessionId);
            if(session.isPresent()) {
                session.get().delete();
            }
        }
        return redirect(routes.HomeController.index());
    }

}
