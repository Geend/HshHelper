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
import play.Logger;
import play.libs.mailer.Email;
import play.libs.mailer.MailerClient;
import policyenforcement.Policy;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import java.util.*;

public class UserManager {
    private final UserFinder userFinder;
    private final GroupFinder groupFinder;
    private final PasswordGenerator passwordGenerator;
    private final MailerClient mailerClient;
    private final HashHelper hashHelper;
    private final EbeanServer ebeanServer;
    private final Policy policy;
    private final SessionManager sessionManager;

    private static final Logger.ALogger logger = Logger.of(UserManager.class);

    @Inject
    public UserManager(
            UserFinder userFinder,
            GroupFinder groupFinder,
            PasswordGenerator passwordGenerator,
            MailerClient mailerClient,
            HashHelper hashHelper,
            EbeanServer server,
            Policy policy,
            SessionManager sessionManager)
    {
        this.groupFinder = groupFinder;
        this.policy = policy;
        this.ebeanServer = server;
        this.mailerClient = mailerClient;
        this.passwordGenerator = passwordGenerator;
        this.userFinder = userFinder;
        this.hashHelper = hashHelper;
        this.sessionManager = sessionManager;
    }

    public String createUser(String username, String email, int quota) throws UnauthorizedException, UsernameAlreadyExistsException, EmailAlreadyExistsException, UsernameCannotBeAdmin {
        User currentUser = sessionManager.currentUser();

        if(!this.policy.CanCreateUser(currentUser)) {
            logger.error(currentUser + " tried to create a user but he is not authorized");
            throw new UnauthorizedException();
        }

        //TODO: Include generated password length in policy
        String plaintextPassword = passwordGenerator.generatePassword(10);
        String passwordHash = hashHelper.hashPassword(plaintextPassword);
        User newUser;
        if(Objects.equals(username.toLowerCase(), "admin")) {
            logger.info(currentUser + " tried to create user with the name \"admin\"");
            throw new UsernameCannotBeAdmin();
        }

        Group allGroup = this.groupFinder.getAllGroup();
        try(Transaction tx = this.ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            if(userFinder.byName(username).isPresent()) {
                logger.error(currentUser + " tried to create user " + username + " but this username already exists");
                throw new UsernameAlreadyExistsException();
            }
            if(userFinder.byEmail(email, UserFinderQueryOptions.CaseInsensitive).isPresent()) {
                logger.error(currentUser + " tried to create user " + username + " but " + email + " already exists");
                throw new EmailAlreadyExistsException();
            }
            newUser = new User(username,
                    email,
                    passwordHash,
                    true,
                    quota);
            List<Group> groups = new ArrayList<>();
            groups.add(allGroup);
            newUser.setGroups(groups);

            this.ebeanServer.save(newUser);
            tx.commit();
            logger.info(currentUser + " created user " + username);
        }
        return plaintextPassword;
    }

    public void deleteUser(Long userToBeDeletedId) throws UnauthorizedException, InvalidArgumentException {
        User currentUser = sessionManager.currentUser();
        Optional<User> userToDelete = this.userFinder.byIdOptional(userToBeDeletedId);

        if(!userToDelete.isPresent())
            throw new InvalidArgumentException("Dieser User existiert nicht.");

        if(!this.policy.CanDeleteUser(currentUser, userToDelete.get())) {
            logger.error(currentUser + " tried to delete a user but he is not authorized");
            throw new UnauthorizedException();
        }
        ebeanServer.delete(userToDelete.get());
        logger.info(currentUser + " deleted user " + userToDelete.get().getUsername());
    }

    public List<User> getAllUsers() throws UnauthorizedException {
        User currentUser = sessionManager.currentUser();

        if(!this.policy.CanViewAllUsers(currentUser)) {
            logger.error(currentUser + " tried to access all users but he is not authorized");
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
        logger.info("Created a new temp pw and send mail for user " + user.getUsername());
    }

    public UserMetaInfo getUserMetaInfo(Long userId) throws UnauthorizedException {
        Optional<User> optUser = userFinder.byIdOptional(userId);
        if(!optUser.isPresent()) {
            throw new IllegalArgumentException();
        }

        User user = optUser.get();

        if(!policy.CanViewUserMetaInfo(sessionManager.currentUser(), user)) {
            throw new UnauthorizedException();
        }

        return new UserMetaInfo(
            user.getUsername(),
            user.getOwnedFiles().size(),
            user.getOwnerOf().size()
        );
    }
}
