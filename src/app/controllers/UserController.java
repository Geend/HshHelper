package controllers;

import extension.*;
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

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static play.libs.Scala.asScala;

public class UserController extends Controller {

    @Inject
    MailerClient mailerClient;

    private UserFinder userFinder;
    private UserSessionFinder userSessionFinder;

    private Form<CreateUserDto> createUserForm;
    private Form<ChangeOwnPasswordDto> changeOwnPasswordForm;
    private Form<ResetUserPasswordDto> resetUserPasswordForm;
    private Form<DeleteSessionDTO> deleteSessionForm;


    @Inject
    public UserController(FormFactory formFactory, UserFinder userFinder, UserSessionFinder userSessionFinder) {
        this.createUserForm = formFactory.form(CreateUserDto.class);
        this.userFinder = userFinder;
        this.userSessionFinder = userSessionFinder;
        this.changeOwnPasswordForm = formFactory.form(ChangeOwnPasswordDto.class);
        this.resetUserPasswordForm = formFactory.form(ResetUserPasswordDto.class);
        this.deleteSessionForm = formFactory.form(DeleteSessionDTO.class);
    }


    @AuthenticationRequired
    public Result showCreateUserForm() {
        return ok(views.html.CreateUser.render(createUserForm));
    }

    @AuthenticationRequired
    public Result createUser() {

        Optional<User> currentUser = ContextArguments.getUser();
        if (!currentUser.isPresent())
            return badRequest("error");

        if (Specification.CanCreateUser(currentUser.get()))
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

        boolean passwordResetRequired = true;

        User newUser = new User(createUserDto.getUsername(),
                createUserDto.getEmail(),
                passwordHash,
                passwordResetRequired,
                createUserDto.getQuotaLimit());


        newUser.save();

        //TODO: Show the create the password
        return ok("created user " + newUser.username + " with inital password " + plaintextPassword);

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

        currentUser.passwordHash = HashHelper.hashPassword(changeOwnPasswordDto.getPassword());

        currentUser.passwordResetRequired = false;
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

        user.passwordHash = HashHelper.hashPassword(tempPassword);
        user.passwordResetRequired = true;

        user.save();

        //TODO: Send an email with the temp password
        //sendPasswordEmail();

        return ok("An email with a temporary password was send to you");

    }

    public void sendPasswordEmail(User user, String tempPassword) {
        Email email = new Email()
                .setSubject("HshHelper Password Rest")
                .setFrom("HshHelper <hshhelper@hs-hannover.de>")
                .addTo(user.email)
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
        Form<DeleteSessionDTO> bf = deleteSessionForm.bindFromRequest();

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
