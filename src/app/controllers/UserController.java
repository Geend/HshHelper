package controllers;

import domainlogic.UnauthorizedException;
import domainlogic.usermanager.EmailAlreadyExistsException;
import domainlogic.usermanager.UserManager;
import domainlogic.usermanager.UsernameAlreadyExistsException;
import domainlogic.usermanager.UsernameCannotBeAdmin;
import models.User;
import models.dtos.*;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;
import policy.Specification;
import policy.session.Authentication;
import policy.session.Session;
import policy.session.SessionManager;
import scala.collection.Seq;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
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

        User currentUser = sessionManager.currentUser();
        List<User> users = this.userManager.getAllUsers(currentUser.getUserId());
        List<UserListEntryDto> entries = users
                .stream()
                .map(x -> new UserListEntryDto(x.getUserId(), x.getUsername()))
                .collect(Collectors.toList());
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setIndex(i + 1);
        }
        Seq<UserListEntryDto> scalaEntries = asScala(entries);
        return ok(views.html.UserList.render(scalaEntries));

    }

    @Authentication.Required
    public Result deleteUser() throws UnauthorizedException {
        User currentUser = sessionManager.currentUser();
        Form<UserIdDto> boundForm = this.deleteUserForm.bindFromRequest("userId");
        if (boundForm.hasErrors()) {
            return badRequest();
        }
        Long userToDeleteId = boundForm.get().getUserId();

        this.userManager.deleteUser(currentUser.getUserId(), userToDeleteId);

        return redirect(routes.UserController.showUsers());
    }


    @Authentication.Required
    public Result showCreateUserForm() throws UnauthorizedException {
        User currentUser = sessionManager.currentUser();
        if (!Specification.instance.CanCreateUser(currentUser)) {
            throw new UnauthorizedException();
        }
        return ok(views.html.CreateUser.render(createUserForm));
    }

    @Authentication.Required
    public Result createUser() throws UnauthorizedException {

        User currentUser = sessionManager.currentUser();
        Form<CreateUserDto> boundForm = createUserForm.bindFromRequest("username", "email", "quotaLimit");
        if (boundForm.hasErrors()) {
            return ok(views.html.CreateUser.render(boundForm));
        }
        CreateUserDto createUserDto = boundForm.get();
        try {
            String plaintextPassword = this.userManager.createUser(
                    currentUser.getUserId(),
                    createUserDto.getUsername(),
                    createUserDto.getEmail(),
                    createUserDto.getQuotaLimit());

            UserCreatedDto userCreatedDto = new UserCreatedDto();
            userCreatedDto.setUsername(createUserDto.getUsername());
            userCreatedDto.setPlaintextPassword(plaintextPassword);
            return ok(views.html.UserCreated.render(userCreatedDto));
        } catch (EmailAlreadyExistsException e) {
            boundForm = boundForm.withError("username", "Existiert bereits");
            return badRequest(views.html.CreateUser.render(boundForm));
        } catch (UsernameAlreadyExistsException e) {
            return unauthorized();
        } catch (UsernameCannotBeAdmin usernameCannotBeAdmin) {
            boundForm = boundForm.withError("username", "username darf nicht admin sein");
            return badRequest(views.html.CreateUser.render(boundForm));
        }
    }

    @Authentication.NotAllowed
    public Result showResetUserPasswordForm() {
        return ok(views.html.ResetUserPassword.render(resetUserPasswordForm));
    }

    @Authentication.NotAllowed
    public Result resetUserPassword() {
        //TODO: Add brute force and/or dos protection

        Form<ResetUserPasswordDto> boundForm = resetUserPasswordForm.bindFromRequest("username");
        if (boundForm.hasErrors()) {
            return ok(views.html.ResetUserPassword.render(boundForm));
        }
        ResetUserPasswordDto resetUserPasswordDto = boundForm.get();
        try {
            this.userManager.resetPassword(resetUserPasswordDto.getUsername());
        } catch (IllegalArgumentException e) {
            //Ignore the exception in order to not reveal potential usernames.
        }


        return ok("An email with a temporary password was send to you");
    }


    @Authentication.Required
    public Result showActiveUserSessions() {
        User u = sessionManager.currentUser();
        List<Session> userSessions = sessionManager.sessionsByUser(u);
        return ok(views.html.UserSessions.render(asScala(userSessions), deleteSessionForm));
    }

    @Authentication.Required
    public Result deleteUserSession() {
        Form<DeleteSessionDto> bf = deleteSessionForm.bindFromRequest();

        Optional<Session> session = sessionManager.getUserSession(sessionManager.currentUser(), bf.get().getSessionId());
        if (!session.isPresent()) {
            return badRequest();
        }

        if (!policy.Specification.instance.CanDeleteSession(sessionManager.currentUser(), session.get())) {
            return badRequest();
        }

        session.get().destroy();

        return redirect(routes.UserController.showActiveUserSessions());
    }
}
