package controllers;

import extension.PasswordGenerator;
import models.User;
import models.dtos.CreateUserDto;
import org.mindrot.jbcrypt.BCrypt;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;

public class UserController extends Controller {


    private Form<CreateUserDto> createUserForm;

    @Inject
    public UserController(FormFactory formFactory) {
        createUserForm = formFactory.form(CreateUserDto.class);
    }


    public Result showCreateUserForm() {
        return ok(views.html.CreateUser.render(createUserForm));
    }


    public Result createUser() {

        //TODO: Check if the logged in user is allowd to create users
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
        return ok("created user "+ newUser.username + " with inital password " + plaintextPassword);

    }


}
