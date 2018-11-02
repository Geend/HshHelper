import domainlogic.UnauthorizedException;
import domainlogic.usermanager.EmailAlreadyExistsException;
import domainlogic.usermanager.UserManager;
import domainlogic.usermanager.UsernameAlreadyExistsException;
import extension.HashHelper;
import extension.PasswordGenerator;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import models.User;
import models.finders.UserFinder;
import org.apache.xpath.operations.Bool;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import play.libs.mailer.MailerClient;
import policy.Specification;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserManagerTest {


    UserManager userManager;
    User adminUser;
    MailerClient mailerClient;
    EbeanServer defaultServer;
    Specification defaultSpecification;

    @Before
    public void init() {
        defaultSpecification = mock(Specification.class);
        when(defaultSpecification.CanCreateUser(any())).thenReturn(true);
        mailerClient = mock(MailerClient.class);
        adminUser = mock(User.class);
        defaultServer = mock(EbeanServer.class);
        when(defaultServer.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));
        when(adminUser.isAdmin()).thenReturn(true);
    }


    @Test
    public void testChangePassword() {

        String testUsername = "test";
        String newPassword = "0123456789";
        String newPasswordHash = "abcdefg";


        HashHelper hashHelper = mock(HashHelper.class);
        when(hashHelper.hashPassword(newPassword)).thenReturn(newPasswordHash);

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        when(passwordGenerator.generatePassword(10)).thenReturn(newPassword);


        User user = mock(User.class);
        when(user.getUsername()).thenReturn(testUsername);

        UserFinder userFinder = mock(UserFinder.class);
        when(userFinder.byName(testUsername)).thenReturn(Optional.of(user));

        userManager = new UserManager(userFinder, passwordGenerator, mailerClient, hashHelper, defaultServer, defaultSpecification);

        userManager.resetPassword(testUsername);

        verify(user).setPasswordHash(hashHelper.hashPassword(newPassword));
        verify(user).setIsPasswordResetRequired(true);
    }

    @Test(expected = UnauthorizedException.class)
    public void createUserObeysSpecification() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException {
        Specification spec = mock(Specification.class);
        when(spec.CanCreateUser(any(User.class))).thenReturn(false);
        UserFinder userFinder = mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(userFinder, passwordGenerator, mailerClient, hashHelper, defaultServer, defaultSpecification);
        sut.createUser(1l, "klaus", "test@test.de", 5);
    }

    @Test(expected = UsernameAlreadyExistsException.class)
    public void createUsernameHasToBeUnique() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException {
        UserFinder userFinder = mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);
        User klausUser = mock(User.class);
        when(userFinder.byName("klaus")).thenReturn(Optional.of(klausUser));
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(userFinder, passwordGenerator, mailerClient, hashHelper, defaultServer, defaultSpecification);
        sut.createUser(1l, "klaus", "test@test.de", 5);
    }

    @Test
    public void testChangePasswordWithNullInput(){

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserFinder userFinder= mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);

        userManager = new UserManager(userFinder, passwordGenerator, mailerClient, hashHelper, defaultServer, defaultSpecification);

        userManager.resetPassword(null);
    }

    @Test
    public void testDeleteUser() throws UnauthorizedException {

        UserFinder userFinder = mock(UserFinder.class);

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        HashHelper hashHelper = mock(HashHelper.class);

        userManager = new UserManager(userFinder, passwordGenerator, mailerClient, hashHelper, defaultServer, defaultSpecification);

        Long adminUserId = 0l;
        when(userFinder.byId(adminUserId)).thenReturn(adminUser);

        Long userToBeDeletedId = 10l;

        User userToBeDeleted = mock(User.class);
        when(userToBeDeleted.getUserId()).thenReturn(userToBeDeletedId);
        when(userFinder.byId(userToBeDeletedId)).thenReturn(userToBeDeleted);


        userManager.deleteUser(adminUserId, userToBeDeletedId);

        verify(defaultServer,times(1)).delete(userToBeDeleted);

    }

    @Test(expected = UnauthorizedException.class)
    public void testDeleteUserWithUnauthorizedUser() throws UnauthorizedException {

        UserFinder userFinder = mock(UserFinder.class);

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        HashHelper hashHelper = mock(HashHelper.class);

        userManager = new UserManager(userFinder, passwordGenerator, mailerClient, hashHelper, defaultServer, defaultSpecification);

        Long unauthorizedUserId = 0l;
        when(userFinder.byId(unauthorizedUserId)).thenReturn(mock(User.class));

        Long userToBeDeletedId = 10l;

        User userToBeDeleted = mock(User.class);
        when(userToBeDeleted.getUserId()).thenReturn(userToBeDeletedId);
        when(userFinder.byId(userToBeDeletedId)).thenReturn(userToBeDeleted);

        userManager.deleteUser(unauthorizedUserId, userToBeDeletedId);

        verify(defaultServer, never()).delete(userToBeDeleted);

    }

}
