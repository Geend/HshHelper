package controllers;

import constants.CookieConstants;
import extension.HashHelper;
import models.User;
import models.UserSession;
import models.dtos.UserLoginDto;
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
    // todo: noch via dependency incjection reingeben
    private UserSessionFinder userSessionFinder;

    @Inject
    public LoginController(FormFactory formFactory) {
        this.loginForm = formFactory.form(UserLoginDto.class);
        this.userSessionFinder = new UserSessionFinder();
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

        if (User.find.authenticate(loginData.getUsername(), loginData.getPassword())) {
            Logger.info("User authenticated");

            String remoteIp = request().remoteAddress();
            User user = User.find.byName(loginData.getUsername()).get();
            UserSession userSession = new UserSession();
            userSession.setConnectedFrom(remoteIp);
            userSession.setUserId(user.getId());
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
