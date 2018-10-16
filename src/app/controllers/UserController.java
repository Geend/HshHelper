package controllers;

import models.User;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;

public class UserController extends Controller {


    private Form<User> createUserForm;

    @Inject
    public UserController(FormFactory formFactory) {
        createUserForm = formFactory.form(User.class);
    }



    public Result create(){
        return ok(views.html.CreateUser.render(createUserForm));
    }
    public Result save() {
        Form<User> boundForm = createUserForm.bindFromRequest("userName", "email", "quotaLimit");

        if (boundForm.hasErrors()) {
            return ok(views.html.CreateUser.render(boundForm));
        }

        User newUser = boundForm.get();

        newUser.save();

        return redirect(routes.UserController.create());

    }



}
