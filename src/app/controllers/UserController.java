package controllers;

import extension.*;
import io.ebean.Ebean;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import models.User;
import models.UserSession;
import models.dtos.*;
import models.finders.UserFinder;
import models.finders.UserSessionFinder;
import play.data.Form;
import play.data.FormFactory;
import play.libs.mailer.Email;
import play.libs.mailer.MailerClient;
import play.mvc.Controller;
import play.mvc.Result;
import policy.Specification;
import scala.collection.Seq;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static play.libs.Scala.asScala;

public class UserController extends Controller {

    @Inject
    MailerClient mailerClient;

    private UserFinder userFinder;
    private UserSessionFinder userSessionFinder;

    private Form<CreateUserDto> createUserForm;
    private Form<ChangeOwnPasswordDto> changeOwnPasswordForm;
    private Form<ResetUserPasswordDto> resetUserPasswordForm;
    private Form<DeleteSessionDto> deleteSessionForm;
    private Form<DeleteUserDto> deleteUserForm;


    @Inject
    public UserController(FormFactory formFactory, UserFinder userFinder, UserSessionFinder userSessionFinder) {
        this.createUserForm = formFactory.form(CreateUserDto.class);
        this.userFinder = userFinder;
        this.userSessionFinder = userSessionFinder;
        this.changeOwnPasswordForm = formFactory.form(ChangeOwnPasswordDto.class);
        this.resetUserPasswordForm = formFactory.form(ResetUserPasswordDto.class);
        this.deleteSessionForm = formFactory.form(DeleteSessionDto.class);
        this.deleteUserForm = formFactory.form(DeleteUserDto.class);

    }


    @AuthenticationRequired
    public Result showUsers() {
        User currentUser = ContextArguments.getUser().get();
        if(!Specification.CanViewAllUsers(currentUser)) {
            return unauthorized();
        }

        List<UserListEntryDto> entries = this.userFinder
                .all()
                .stream()
                .map(x -> new UserListEntryDto(x.getUserId(), x.getUsername()))
                .collect(Collectors.toList());

        for(int i = 0; i < entries.size(); i++) {
            entries.get(i).setIndex(i + 1);
        }
        Seq<UserListEntryDto> scalaEntries = asScala(entries);
        return ok(views.html.UserList.render(scalaEntries));
    }

    @AuthenticationRequired
    public Result deleteUser()
    {
        User currentUser = ContextArguments.getUser().get();
        Form<DeleteUserDto> boundForm = this.deleteUserForm.bindFromRequest("userId");
        if(boundForm.hasErrors()) {
            return badRequest();
        }
        User userToDelete = this.userFinder.byId(boundForm.get().getUserId());
        if(!Specification.CanDeleteUser(currentUser, userToDelete)) {
            return unauthorized();
        }
        userToDelete.delete();

        Email email = new Email()
                .setSubject("HshHelper Account Deleted")
                .setFrom("HshHelper <hshhelper@hs-hannover.de>")
                .addTo(userToDelete.getEmail())
                .setBodyText("Your account was deleted");

        //TODO: Catch possible exception (eg if the mail server is down)
        mailerClient.send(email);


        return redirect(routes.UserController.showUsers());
    }


    @AuthenticationRequired
    public Result showCreateUserForm() {
        User currentUser = ContextArguments.getUser().get();
        if(!Specification.CanCreateUser(currentUser)) {
            return unauthorized();
        }
        return ok(views.html.CreateUser.render(createUserForm));
    }

    @AuthenticationRequired
    public Result createUser() {

        User currentUser = ContextArguments.getUser().get();

        if (!Specification.CanCreateUser(currentUser))
            return badRequest("error");


        Form<CreateUserDto> boundForm = createUserForm.bindFromRequest("username", "email", "quotaLimit");

        if (boundForm.hasErrors()) {
            return ok(views.html.CreateUser.render(boundForm));
        }

        CreateUserDto createUserDto = boundForm.get();
        PasswordGenerator passwordGenerator = new PasswordGenerator();

        //TODO: Include generated password length in policy
        String plaintextPassword = passwordGenerator.generatePassword(10);
        String passwordHash = HashHelper.hashPassword(plaintextPassword);

        User newUser;
        try(Transaction tx = Ebean.beginTransaction(TxIsolation.REPEATABLE_READ)) {
            if(userFinder.byName(createUserDto.getUsername()).isPresent()) {
                boundForm = boundForm.withError("username", "Existiert bereits");
                return badRequest(views.html.CreateUser.render(boundForm));
            }

            newUser = new User(createUserDto.getUsername(),
                    createUserDto.getEmail(),
                    passwordHash,
                    true,
                    createUserDto.getQuotaLimit());
            newUser.save();

            tx.commit();
        }

        UserCreatedDto userCreatedDto = new UserCreatedDto();
        userCreatedDto.setUsername(newUser.getUsername());
        userCreatedDto.setPlaintextPassword(passwordGenerator.generatePassword(10));

        return ok(views.html.UserCreated.render(userCreatedDto));
    }




    @AuthenticationRequired
    public Result showChangeOwnPasswordForm() {
        return ok(views.html.ChangePassword.render(changeOwnPasswordForm));
    }

    @AuthenticationRequired
    public Result changeOwnPassword() {
        Optional<User> userOptional = ContextArguments.getUser();
        if (!userOptional.isPresent())
            return badRequest("error");

        User currentUser = userOptional.get();


        if (!Specification.CanChangePassword(currentUser, currentUser))
            return badRequest("error");


        Form<ChangeOwnPasswordDto> boundForm = changeOwnPasswordForm.bindFromRequest("password", "passwordRepeat");

        if (boundForm.hasErrors()) {
            return ok(views.html.ChangePassword.render(boundForm));
        }

        ChangeOwnPasswordDto changeOwnPasswordDto = boundForm.get();

        currentUser.setPasswordHash(HashHelper.hashPassword(changeOwnPasswordDto.getPassword()));
        currentUser.setIsPasswordResetRequired(false);
        currentUser.save();

        return ok("changedPassword");
    }

    public Result showResetUserPasswordForm() {
        return ok(views.html.ResetUserPassword.render(resetUserPasswordForm));
    }


    public Result resetUserPassword() {
        //TODO: Add brute force and/or dos protection

        Form<ResetUserPasswordDto> boundForm = resetUserPasswordForm.bindFromRequest("username");

        if (boundForm.hasErrors()) {
            return ok(views.html.ResetUserPassword.render(boundForm));
        }

        ResetUserPasswordDto resetUserPasswordDto = boundForm.get();

        String username = resetUserPasswordDto.getUsername();

        Optional<User> userOptional = userFinder.byName(username);


        if (!userOptional.isPresent())
            return ok("An email with a temporary password was send to you");


        User user = userOptional.get();

        PasswordGenerator passwordGenerator = new PasswordGenerator();

        //TODO: Include generated password length in policy
        String tempPassword = passwordGenerator.generatePassword(10);

        user.setPasswordHash(HashHelper.hashPassword(tempPassword));
        user.setIsPasswordResetRequired(true);
        user.save();

        sendPasswordEmail(user, tempPassword);

        return ok("An email with a temporary password was send to you");

    }

    public void sendPasswordEmail(User user, String tempPassword) {
        Email email = new Email()
                .setSubject("HshHelper Password Rest")
                .setFrom("HshHelper <hshhelper@hs-hannover.de>")
                .addTo(user.getEmail())
                .setBodyText("Your temp password is " + tempPassword);

        //TODO: Catch possible exception (eg if the mail server is down)
        mailerClient.send(email);
    }

    @AuthenticationRequired
    public Result showActiveUserSessions() {
        User u = ContextArguments.getUser().get();
        List<UserSession> userSessions = userSessionFinder.byUser(u);
        return ok(views.html.UserSessions.render(asScala(userSessions), deleteSessionForm));
    }

    @AuthenticationRequired
    public Result deleteUserSession() {
        Form<DeleteSessionDto> bf = deleteSessionForm.bindFromRequest();

        Optional<UserSession> session = userSessionFinder.byIdOptional(bf.get().getSessionId());
        if(!session.isPresent()) {
            return badRequest();
        }

        if(!policy.Specification.CanDeleteSession(ContextArguments.getUser().get(), session.get())) {
            return badRequest();
        }

        session.get().delete();

        return redirect(routes.UserController.showActiveUserSessions());
    }
}
