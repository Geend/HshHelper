package controllers;

import constants.CookieConstants;
import extension.AuthenticateUser;
import extension.HashHelper;
import models.User;
import models.UserSession;
import models.dtos.UserLoginDto;
import models.finders.UserFinder;
import models.finders.UserSessionFinder;
import org.joda.time.DateTime;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;


import javax.inject.Inject;
import java.util.Optional;

public class LoginController extends Controller {

    private Form<UserLoginDto> loginForm;
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
        this.userSessionFinder = userSessionFinder;
        this.userFinder = userFinder;
        this.authenticateUser = authenticateUser;
    }

    public Result login() {
        return ok(views.html.Login.render(loginForm));
    }

    public Result loginCommit() {
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
                return TODO;
            }

            Logger.info("User authenticated");

            String remoteIp = request().remoteAddress();
            UserSession userSession = new UserSession();
            userSession.setConnectedFrom(remoteIp);
            userSession.setUserId(user.get().getId());
            userSession.setIssuedAt(DateTime.now());
            userSession.save();

            session().put(CookieConstants.USER_SESSION_ID_NAME, userSession.getId().toString());

            return redirect(routes.HelloWorldController.index());
        }

        Logger.info("User could not be authenticated");
        return redirect(routes.LoginController.login());
    }

    public Result logoutCommit() {

        if (session().containsKey(CookieConstants.USER_SESSION_ID_NAME)) {

            String sessionIdString = session().getOrDefault(CookieConstants.USER_SESSION_ID_NAME, null);
            session().remove(CookieConstants.USER_SESSION_ID_NAME);

            Long sessionId = Long.parseLong(sessionIdString);
            Optional<UserSession> session = this.userSessionFinder.byIdOptional(sessionId);
            if(session.isPresent()) {
                session.get().delete();
            }
        }
        return redirect(routes.HelloWorldController.index());
    }

}
