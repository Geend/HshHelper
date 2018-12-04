package managers.usermanager;

import extension.*;
import extension.logging.DangerousCharFilteringLogger;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.WeakPasswordException;
import models.Group;
import models.User;
import models.factories.UserFactory;
import models.finders.GroupFinder;
import models.finders.UserFinder;
import models.finders.UserFinderQueryOptions;
import play.Logger;
import play.libs.mailer.MailerClient;
import policyenforcement.ConstraintValues;
import policyenforcement.session.SessionManager;
import twofactorauth.TimeBasedOneTimePasswordUtil;

import javax.inject.Inject;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static policyenforcement.ConstraintValues.TIME_WINDOW_2FA_MS;

public class UserManager {
    private final UserFinder userFinder;
    private final GroupFinder groupFinder;
    private final PasswordGenerator passwordGenerator;
    private final MailerClient mailerClient;
    private final HashHelper hashHelper;
    private final EbeanServer ebeanServer;
    private final SessionManager sessionManager;
    private final RecaptchaHelper recaptchaHelper;
    private final UserFactory userFactory;
    private final CredentialManager credentialManager;
    private final WeakPasswords weakPasswords;

    private static final Logger.ALogger logger = new DangerousCharFilteringLogger(UserManager.class);

    @Inject
    public UserManager(
            UserFinder userFinder,
            GroupFinder groupFinder,
            PasswordGenerator passwordGenerator,
            MailerClient mailerClient,
            HashHelper hashHelper,
            EbeanServer server,
            SessionManager sessionManager,
            RecaptchaHelper recaptchaHelper,
            UserFactory userFactory, CredentialManager credentialManager, WeakPasswords weakPasswords)
    {
        this.groupFinder = groupFinder;
        this.ebeanServer = server;
        this.mailerClient = mailerClient;
        this.passwordGenerator = passwordGenerator;
        this.userFinder = userFinder;
        this.hashHelper = hashHelper;
        this.sessionManager = sessionManager;
        this.recaptchaHelper = recaptchaHelper;
        this.userFactory = userFactory;
        this.credentialManager = credentialManager;
        this.weakPasswords = weakPasswords;
    }

    public void activateTwoFactorAuth(String secret, String activationToken) throws Invalid2FATokenException {
        try {
            String tokenWithoutWhiteSpace = activationToken.replaceAll(" ", "");
            int intToken = Integer.parseInt(tokenWithoutWhiteSpace);

            if(!TimeBasedOneTimePasswordUtil.validateCurrentNumber(secret, intToken, TIME_WINDOW_2FA_MS))
                throw new Invalid2FATokenException();
        } catch (GeneralSecurityException | NumberFormatException e) {
            throw new Invalid2FATokenException();
        }

        User currentUser = sessionManager.currentUser();
        currentUser.setTwoFactorAuthSecret(secret);
        this.ebeanServer.save(currentUser);
    }

    public void deactivateTwoFactorAuth() throws InvalidArgumentException, UnauthorizedException {
        Long currentUid = sessionManager.currentUser().getUserId();
        deactivateTwoFactorAuth(currentUid);
    }

    public void deactivateTwoFactorAuth(Long userId) throws InvalidArgumentException, UnauthorizedException {
        Optional<User> optUser = userFinder.byIdOptional(userId);
        if(!optUser.isPresent()) {
            throw new InvalidArgumentException("Ungültige userId");
        }

        User user = optUser.get();
        if(!sessionManager.currentPolicy().canDisable2FA(user)) {
            throw new UnauthorizedException();
        }

        user.setTwoFactorAuthSecret("");

        this.ebeanServer.save(user);
    }

    public String generateTwoFactorSecret() {
        String temporarySecret = TimeBasedOneTimePasswordUtil.generateBase32Secret();
        return temporarySecret;
    }

    public String createUser(String username, String email, Long quota) throws UnauthorizedException, UsernameAlreadyExistsException, EmailAlreadyExistsException, UsernameCannotBeAdmin {
        User currentUser = sessionManager.currentUser();

        if(!sessionManager.currentPolicy().canCreateUser()) {
            logger.error(currentUser + " tried to create a user but he is not authorized");
            throw new UnauthorizedException();
        }

        String plaintextPassword = passwordGenerator.generatePassword(ConstraintValues.GENREATED_PASSWORD_LENGTH);
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
            newUser = userFactory.CreateUser(
                    username,
                    email,
                    plaintextPassword,
                    true,
                    quota
            );
            List<Group> groups = new ArrayList<>();
            groups.add(allGroup);
            newUser.setGroups(groups);

            this.ebeanServer.save(newUser);
            tx.commit();
            logger.info(currentUser + " created user " + newUser);
        }
        return plaintextPassword;
    }

    public void deleteUser(Long userToBeDeletedId) throws UnauthorizedException, InvalidArgumentException {
        User currentUser = sessionManager.currentUser();
        Optional<User> userToDeleteOpt = this.userFinder.byIdOptional(userToBeDeletedId);

        if(!userToDeleteOpt.isPresent())
            throw new InvalidArgumentException("Dieser User existiert nicht.");

        User userToDelete = userToDeleteOpt.get();

        if(!sessionManager.currentPolicy().canDeleteUser(userToDelete)) {
            logger.error(currentUser  + " tried to delete a user but he is not authorized");
            throw new UnauthorizedException();
        }

        sessionManager.destroyAllUserSessions(userToDelete);
        ebeanServer.delete(userToDelete);
        logger.info(currentUser + " deleted user " + userToDelete);
    }

    public List<User> getAllUsers() throws UnauthorizedException {
        User currentUser = sessionManager.currentUser();

        if(!sessionManager.currentPolicy().canViewAllUsers()) {
            logger.error(currentUser + " tried to access all users but he is not authorized");
            throw new UnauthorizedException();
        }
        return this.userFinder.all();
    }

    public List<User> getAdminUsers() throws UnauthorizedException {
        User currentUser = sessionManager.currentUser();

        if(!sessionManager.currentPolicy().canViewAllUsers()) {
            logger.error(currentUser + " tried to access all users but he is not authorized");
            throw new UnauthorizedException();
        }

        // TODO: Extract the grp name as static var
        return this.userFinder.query().where()
                .eq("groups.name", "Administrators")
                .findList();
    }
    
    public UserMetaInfo getUserMetaInfo(Long userId) throws UnauthorizedException, InvalidArgumentException {
        Optional<User> optUser = userFinder.byIdOptional(userId);
        if(!optUser.isPresent()) {
            throw new InvalidArgumentException("Dieser User existiert nicht.");
        }

        User user = optUser.get();

        if(!sessionManager.currentPolicy().canViewUserMetaInfo()) {
            throw new UnauthorizedException();
        }

        return new UserMetaInfo(
            user.getUsername(),
            user.getOwnerOf().size(),
            user.has2FA()
        );
    }

    public String getUsername(Long userId) throws InvalidArgumentException {
        Optional<User> optUser = userFinder.byIdOptional(userId);
        if(!optUser.isPresent()) {
            throw new InvalidArgumentException("Dieser User existiert nicht.");
        }

        User user = optUser.get();
        return user.getUsername();
    }

    public void changeUserSessionTimeout(Integer valueInMinutes) throws UnauthorizedException, InvalidArgumentException {
        User user = sessionManager.currentUser();

        if(!sessionManager.currentPolicy().canChangeUserTimeoutValue(user)){
            throw new UnauthorizedException();
        }

        if(valueInMinutes == null){
            throw new InvalidArgumentException();
        }

        user.setSessionTimeoutInMinutes(valueInMinutes);

        ebeanServer.save(user);
    }

    public void changeUserPassword(String currentPassword, String newPassword) throws UnauthorizedException, WeakPasswordException {
        if(weakPasswords.isWeakPw(newPassword))
            throw new WeakPasswordException();

        try(Transaction tx = this.ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            User user = sessionManager.currentUser();

            if (!hashHelper.checkHash(currentPassword, user.getPasswordHash())) {
                throw new UnauthorizedException();
            }

            user.setPasswordHash(hashHelper.hashPassword(newPassword));

            ebeanServer.save(user);

            credentialManager.updateCredentialPassword(currentPassword, newPassword);

            tx.commit();
        }
    }

    public Long getUserQuotaLimit(Long userId) throws UnauthorizedException, InvalidArgumentException {
        User currentUser = sessionManager.currentUser();

        if(!sessionManager.currentPolicy().canReadWriteQuotaLimit()){
            throw new UnauthorizedException();
        }

        Optional<User> user = userFinder.byIdOptional(userId);

        if(!user.isPresent()){
            throw new InvalidArgumentException();
        }

        return user.get().getQuotaLimit();
    }

    public void changeUserQuotaLimit(Long userId, Long newQuotaLimit) throws UnauthorizedException, InvalidArgumentException {
        User currentUser = sessionManager.currentUser();

        if(!sessionManager.currentPolicy().canReadWriteQuotaLimit()){
            throw new UnauthorizedException();
        }

        Optional<User> user = userFinder.byIdOptional(userId);
        if(!user.isPresent()){
            throw new InvalidArgumentException();
        }
        user.get().setQuotaLimit(newQuotaLimit);
        ebeanServer.save(user.get());
    }
}
