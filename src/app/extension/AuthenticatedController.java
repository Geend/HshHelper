package extension;

import models.User;
import play.mvc.Controller;

import javax.sound.sampled.Control;

public class AuthenticatedController extends Controller {
    public User getCurrentUser() {
        return User.findAll().get(0);
    }
}
