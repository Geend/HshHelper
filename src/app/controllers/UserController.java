package controllers;

import extension.AuthenticationRequired;
import extension.ContextArguments;
import extension.HashHelper;
import extension.PasswordGenerator;
import models.User;
import models.dtos.ChangeOwnPasswordDto;
import models.dtos.CreateUserDto;
import models.dtos.ResetUserPasswordDto;
import models.finders.UserFinder;
import play.data.Form;
import play.data.FormFactory;
import play.libs.mailer.Email;
import play.libs.mailer.MailerClient;
import play.mvc.Controller;
import play.mvc.Result;
import policy.Specification;

import javax.inject.Inject;
import java.util.Optional;

public class UserController extends Controller {

    @Inject
    MailerClient mailerClient;

    private UserFinder userFinder;
    private Form<CreateUserDto> createUserForm;
    private Form<ChangeOwnPasswordDto> changeOwnPasswordForm;
    private Form<ResetUserPasswordDto> resetUserPasswordForm;


    @Inject
    public UserController(FormFactory formFactory, UserFinder userFinder) {
        this.createUserForm = formFactory.form(CreateUserDto.class);
        this.userFinder = userFinder;
        this.changeOwnPasswordForm = formFactory.form(ChangeOwnPasswordDto.class);
        this.resetUserPasswordForm = formFactory.form(ResetUserPasswordDto.class);
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

        String plaintextPassword = passwordGenerator.generatePassword();
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
        String tempPassword = passwordGenerator.generatePassword();

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
}
