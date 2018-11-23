package controllers;

import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.loginmanager.CaptchaRequiredException;
import managers.usermanager.*;
import models.User;
import dtos.*;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import policyenforcement.Policy;
import policyenforcement.session.Authentication;
import policyenforcement.session.Session;
import policyenforcement.session.SessionManager;
import scala.collection.Seq;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static play.libs.Scala.asScala;

public class UserController extends Controller {

    private final UserManager userManager;
    private final Form<CreateUserDto> createUserForm;
    private final Form<ResetUserPasswordDto> resetUserPasswordForm;
    private final Form<DeleteSessionDto> deleteSessionForm;
    private final Form<UserIdDto> deleteUserForm;
    private final Form<ChangeUserSessionTimeoutDto> changeUserSessionTimeoutForm;
    private final SessionManager sessionManager;


    @Inject
    public UserController(FormFactory formFactory, UserManager userManager, SessionManager sessionManager) {
        this.userManager = userManager;
        this.createUserForm = formFactory.form(CreateUserDto.class);
        this.resetUserPasswordForm = formFactory.form(ResetUserPasswordDto.class);
        this.deleteSessionForm = formFactory.form(DeleteSessionDto.class);
        this.deleteUserForm = formFactory.form(UserIdDto.class);
        this.changeUserSessionTimeoutForm = formFactory.form(ChangeUserSessionTimeoutDto.class);
        this.sessionManager = sessionManager;
    }

    @Authentication.Required
    public Result showUsers() throws UnauthorizedException {
        List<User> users = this.userManager.getAllUsers();
        return ok(views.html.users.Users.render(asScala(users)));
    }

    @Authentication.Required
    public Result showAdminUsers() throws UnauthorizedException {
        List<User> users = this.userManager.getAllUsers();
        // TODO: db query instead local filtering
        users = users.stream().filter(User::isAdmin).collect(Collectors.toList());
        return ok(views.html.users.Users.render(asScala(users)));
    }

    @Authentication.Required
    public Result showConfirmDeleteForm() throws UnauthorizedException, InvalidArgumentException {
        Form<UserIdDto> boundForm = this.deleteUserForm.bindFromRequest("userId");
        if (boundForm.hasErrors()) {
            return badRequest();
        }

        Long userToDeleteId = boundForm.get().getUserId();
        UserMetaInfo info = userManager.getUserMetaInfo(userToDeleteId);

        return ok(views.html.users.DeleteUserConfirmation.render(info, userToDeleteId));
    }


    @Authentication.Required
    public Result deleteUser() throws UnauthorizedException, InvalidArgumentException {
        Form<UserIdDto> boundForm = this.deleteUserForm.bindFromRequest("userId");
        if (boundForm.hasErrors()) {
            return badRequest();
        }
        Long userToDeleteId = boundForm.get().getUserId();

        this.userManager.deleteUser(userToDeleteId);

        return redirect(routes.UserController.showUsers());
    }


    @Authentication.Required
    public Result showCreateUserForm() throws UnauthorizedException {
        User currentUser = sessionManager.currentUser();
        if (!Policy.instance.CanCreateUser(currentUser)) {
            throw new UnauthorizedException();
        }
        return ok(views.html.users.CreateUser.render(createUserForm));
    }

    @Authentication.Required
    public Result createUser() throws UnauthorizedException, InvalidArgumentException {
        Form<CreateUserDto> boundForm = createUserForm.bindFromRequest("username", "email", "quotaLimit");
        if (boundForm.hasErrors()) {
            return ok(views.html.users.CreateUser.render(boundForm));
        }
        CreateUserDto createUserDto = boundForm.get();
        try {
            String plaintextPassword = this.userManager.createUser(
                    createUserDto.getUsername(),
                    createUserDto.getEmail(),
                    createUserDto.getQuotaLimit());

            UserCreatedDto userCreatedDto = new UserCreatedDto();
            userCreatedDto.setUsername(createUserDto.getUsername());
            userCreatedDto.setPlaintextPassword(plaintextPassword);
            return ok(views.html.users.UserCreated.render(userCreatedDto));
        } catch (EmailAlreadyExistsException e) {
            boundForm = boundForm.withError("email", "Email already in use");
            return badRequest(views.html.users.CreateUser.render(boundForm));
        } catch (UsernameAlreadyExistsException e) {
            throw new InvalidArgumentException(e.getMessage());
        } catch (UsernameCannotBeAdmin usernameCannotBeAdmin) {
            boundForm = boundForm.withError("username", "Username must not be admin");
            return badRequest(views.html.users.CreateUser.render(boundForm));
        }
    }

    @Authentication.NotAllowed
    public Result showResetUserPasswordForm() {
        return ok(views.html.users.ResetUserPassword.render(resetUserPasswordForm));
    }

    @Authentication.NotAllowed
    public Result resetUserPassword() {
        //TODO: Add brute force and/or dos protection

        Form<ResetUserPasswordDto> boundForm = resetUserPasswordForm.bindFromRequest("username");
        if (boundForm.hasErrors()) {
            return ok(views.html.users.ResetUserPassword.render(boundForm));
        }

        ResetUserPasswordDto resetUserPasswordData = boundForm.get();
        Optional<String> recaptchaData = boundForm.field("g-recaptcha-response").getValue();
        if (recaptchaData.isPresent()) {
            resetUserPasswordData.setRecaptcha(recaptchaData.get());
        }

        ResetUserPasswordDto resetUserPasswordDto = boundForm.get();
        try {
            this.userManager.resetPassword(resetUserPasswordDto.getUsername(), resetUserPasswordData.getRecaptcha(), Http.Context.current().request());
        } catch (InvalidArgumentException e) {
            //Ignore the exception in order to not reveal potential usernames.
        } catch (CaptchaRequiredException e) {
            boundForm = boundForm.withGlobalError("Complete the captcha!");
            return badRequest(views.html.users.ResetUserPassword.render(boundForm));
        }

        return ok(views.html.users.ResetUserPasswordResult.render());
    }


    @Authentication.Required
    public Result showActiveUserSessions() {
        User currentUser = sessionManager.currentUser();
        List<Session> activeSessions = sessionManager.sessionsByUser(currentUser);
        return ok(views.html.users.UserSessions.render(asScala(activeSessions), asScala(currentUser.getLoginAttempts())));
    }

    @Authentication.Required
    public Result deleteUserSession() throws InvalidArgumentException, UnauthorizedException {
        Form<DeleteSessionDto> bf = deleteSessionForm.bindFromRequest();
        sessionManager.destroySessionOfCurrentUser(bf.get().getSessionId());
        return redirect(routes.UserController.showActiveUserSessions());
    }


    @Authentication.Required
    public Result showUserSettings() throws InvalidArgumentException, UnauthorizedException {
        ChangeUserSessionTimeoutDto changeUserSessionTimeoutDto = new ChangeUserSessionTimeoutDto();
        changeUserSessionTimeoutDto.setValueInMinutes((int) sessionManager.currentUser().getSessionTimeoutInMinutes());
        Form<ChangeUserSessionTimeoutDto> filledForm = changeUserSessionTimeoutForm.fill(changeUserSessionTimeoutDto);

        return ok(views.html.users.UserSettings.render(filledForm));
    }

    @Authentication.Required
    public Result changeUserSessionTimeout() throws UnauthorizedException, InvalidArgumentException {
        Form<ChangeUserSessionTimeoutDto> boundForm = changeUserSessionTimeoutForm.bindFromRequest("valueInMinutes");

        if(boundForm.hasErrors())
        {
            return redirect(routes.UserController.showUserSettings());
        }

        userManager.changeUserSessionTimeout(boundForm.get().getValueInMinutes());

        return redirect(routes.UserController.showUserSettings());
    }


}
