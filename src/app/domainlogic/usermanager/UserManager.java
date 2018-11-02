package domainlogic.usermanager;

import domainlogic.UnauthorizedException;
import models.User;

import java.util.List;

public class UserManager {

    public String createUser(String username, String email, int quota) throws UnauthorizedException, UsernameAlreadyExistsException, EmailAlreadyExistsException {

    }

    public void deleteUser(Long id) throws UnauthorizedException {

    }

    public List<User> getAllUsers() throws UnauthorizedException {

    }
}
