package controllers;

import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.usermanager.EmailAlreadyExistsException;
import managers.usermanager.UserManager;
import managers.usermanager.UsernameAlreadyExistsException;
import managers.usermanager.UsernameCannotBeAdmin;
import models.User;
import dtos.*;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;
import policyenforcement.Policy;
import policyenforcement.session.Authentication;
import policyenforcement.session.Session;
import policyenforcement.session.SessionManager;
import scala.collection.Seq;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static play.libs.Scala.asScala;

public class UserController extends Controller {

    private UserManager userManager;

    private Form<CreateUserDto> createUserForm;
    private Form<ResetUserPasswordDto> resetUserPasswordForm;
    private Form<DeleteSessionDto> deleteSessionForm;
    private Form<UserIdDto> deleteUserForm;
    private final SessionManager sessionManager;


    @Inject
    public UserController(FormFactory formFactory, UserManager userManager, SessionManager sessionManager) {
        this.userManager = userManager;
        this.createUserForm = formFactory.form(CreateUserDto.class);
        this.resetUserPasswordForm = formFactory.form(ResetUserPasswordDto.class);
        this.deleteSessionForm = formFactory.form(DeleteSessionDto.class);
        this.deleteUserForm = formFactory.form(UserIdDto.class);
        this.sessionManager = sessionManager;
    }

    @Authentication.Required
    public Result showUsers() throws UnauthorizedException {
        List<User> users = this.userManager.getAllUsers();
        List<UserListEntryDto> entries = users
                .stream()
                .map(x -> new UserListEntryDto(x.getUserId(), x.getUsername()))
                .collect(Collectors.toList());
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setIndex(i + 1);
        }
        Seq<UserListEntryDto> scalaEntries = asScala(entries);
        return ok(views.html.users.UserList.render(scalaEntries));

    }

    @Authentication.Required
    public Result deleteUser() throws UnauthorizedException, InvalidArgumentException {
        Form<UserIdDto> boundForm = this.deleteUserForm.bindFromRequest("userId");
        if (boundForm.hasErrors()) {
            return badRequest();
        }
        Long userToDeleteId = boundForm.get().getUserId();

        this.userManager.deleteUser( userToDeleteId);

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
        ResetUserPasswordDto resetUserPasswordDto = boundForm.get();
        try {
            this.userManager.resetPassword(resetUserPasswordDto.getUsername());
        } catch (InvalidArgumentException e) {
            //Ignore the exception in order to not reveal potential usernames.
        }


        return ok(views.html.users.ResetUserPasswordResult.render());
    }


    @Authentication.Required
    public Result showActiveUserSessions() {
        User u = sessionManager.currentUser();
        Map<Session, Form<DeleteSessionDto>> sessionFormMap = sessionManager.sessionsByUser(u)
                .stream()
                .collect(Collectors.toMap(
                        session -> session,
                        s -> deleteSessionForm.fill(new DeleteSessionDto(s.getSessionKey()))
                        ));
        return ok(views.html.users.UserSessions.render(asScala(sessionFormMap)));
    }

    @Authentication.Required
    public Result deleteUserSession() throws InvalidArgumentException, UnauthorizedException {
        Form<DeleteSessionDto> bf = deleteSessionForm.bindFromRequest();
        sessionManager.destroySessionOfCurrentUser(bf.get().getSessionId());
        return redirect(routes.UserController.showActiveUserSessions());
    }
}
