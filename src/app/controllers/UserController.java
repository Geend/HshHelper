package controllers;

import extension.AuthenticationRequired;
import extension.ContextArguments;
import extension.PasswordGenerator;
import models.User;
import models.dtos.ChangeOwnPasswordDto;
import models.dtos.CreateUserDto;
import org.mindrot.jbcrypt.BCrypt;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;
import policy.Specification;

import javax.inject.Inject;
import java.util.Optional;

public class UserController extends Controller {


    private Form<CreateUserDto> createUserForm;
    private Form<ChangeOwnPasswordDto> changeOwnPasswordForm;

    @Inject
    public UserController(FormFactory formFactory) {
        createUserForm = formFactory.form(CreateUserDto.class);
        changeOwnPasswordForm = formFactory.form(ChangeOwnPasswordDto.class);
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

        //TODO: Check if BCrypt.gensalt() returns good salts
        String passwordHash = BCrypt.hashpw(plaintextPassword, BCrypt.gensalt());
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

        //TODO: Check if BCrypt.gensalt() returns good salts
        currentUser.passwordHash = BCrypt.hashpw(changeOwnPasswordDto.getPassword(), BCrypt.gensalt());
        currentUser.passwordResetRequired = false;
        currentUser.save();

        return ok("changedPassword");
    }
}
