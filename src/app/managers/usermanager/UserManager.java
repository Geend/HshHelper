package managers.usermanager;

import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import extension.HashHelper;
import extension.PasswordGenerator;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import models.Group;
import models.User;
import models.finders.GroupFinder;
import models.finders.UserFinder;
import models.finders.UserFinderQueryOptions;
import play.libs.mailer.Email;
import play.libs.mailer.MailerClient;
import policyenforcement.Specification;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class UserManager {
    private UserFinder userFinder;
    private GroupFinder groupFinder;
    private PasswordGenerator passwordGenerator;
    private MailerClient mailerClient;
    private HashHelper hashHelper;
    private EbeanServer ebeanServer;
    private Specification specification;

    @Inject
    public UserManager(
            UserFinder userFinder,
            GroupFinder groupFinder,
            PasswordGenerator passwordGenerator,
            MailerClient mailerClient,
            HashHelper hashHelper,
            EbeanServer server,
            Specification specification)
    {
        this.groupFinder = groupFinder;
        this.specification = specification;
        this.ebeanServer = server;
        this.mailerClient = mailerClient;
        this.passwordGenerator = passwordGenerator;
        this.userFinder = userFinder;
        this.hashHelper = hashHelper;
    }

    public String createUser(Long userId, String username, String email, int quota) throws UnauthorizedException, UsernameAlreadyExistsException, EmailAlreadyExistsException, UsernameCannotBeAdmin {
        User currentUser = this.userFinder.byId(userId);
        if(!this.specification.CanCreateUser(currentUser)) {
            throw new UnauthorizedException();
        }

        //TODO: Include generated password length in policy
        String plaintextPassword = passwordGenerator.generatePassword(10);
        String passwordHash = hashHelper.hashPassword(plaintextPassword);
        User newUser;
        if(Objects.equals(username.toLowerCase(), "admin")) {
            throw new UsernameCannotBeAdmin();
        }

        Group allGroup = this.groupFinder.getAllGroup();
        try(Transaction tx = this.ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            if(userFinder.byName(username).isPresent()) {
                throw new UsernameAlreadyExistsException();
            }
            if(userFinder.byEmail(email, UserFinderQueryOptions.CaseInsensitive).isPresent()) {
                throw new EmailAlreadyExistsException();
            }
            newUser = new User(username,
                    email,
                    passwordHash,
                    true,
                    quota);
            HashSet<Group> groups = new HashSet<>();
            groups.add(allGroup);
            newUser.setGroups(groups);

            this.ebeanServer.save(newUser);
            tx.commit();
        }
        return plaintextPassword;
    }

    public void deleteUser(Long userId, Long userToBeDeletedId) throws UnauthorizedException, InvalidArgumentException {
        Optional<User> currentUser = this.userFinder.byIdOptional(userId);
        Optional<User> userToDelete = this.userFinder.byIdOptional(userToBeDeletedId);

        if(!currentUser.isPresent())
            throw new InvalidArgumentException("Dieser User existiert nicht.");

        if(!userToDelete.isPresent())
            throw new InvalidArgumentException("Dieser User existiert nicht.");


        if(!this.specification.CanDeleteUser(currentUser.get(), userToDelete.get())) {
            throw new UnauthorizedException();
        }
        ebeanServer.delete(userToDelete.get());
    }

    public List<User> getAllUsers(Long userId) throws UnauthorizedException {
        User currentUser = this.userFinder.byId(userId);
        if(!this.specification.CanViewAllUsers(currentUser)) {
            throw new UnauthorizedException();
        }
        return this.userFinder.all();
    }

    public void resetPassword(String username) throws InvalidArgumentException {
        Optional<User> userOptional = userFinder.byName(username);
        if (!userOptional.isPresent()) {
            throw new InvalidArgumentException("Dieser User existiert nicht.");
        }
        User user = userOptional.get();

        //TODO: Include generated password length in policy
        String tempPassword = passwordGenerator.generatePassword(10);
        user.setPasswordHash(hashHelper.hashPassword(tempPassword));
        user.setIsPasswordResetRequired(true);
        user.save();

        Email email = new Email()
                .setSubject("HshHelper Password Rest")
                .setFrom("HshHelper <hshhelper@hs-hannover.de>")
                .addTo(user.getEmail())
                .setBodyText("Your temp password is " + tempPassword);
        mailerClient.send(email);
    }
}
