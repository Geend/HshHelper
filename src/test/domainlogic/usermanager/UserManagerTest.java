package domainlogic.usermanager;

import domainlogic.UnauthorizedException;
import extension.HashHelper;
import extension.PasswordGenerator;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import models.User;
import models.finders.UserFinder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.libs.mailer.MailerClient;
import policy.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserManagerTest {

    User adminUser;
    UserManager userManager;
    UserFinder defaultUserFinder;
    MailerClient defaultMailerClient;
    EbeanServer defaultServer;
    Specification defaultSpecification;

    @Before
    public void init() {
        defaultUserFinder = mock(UserFinder.class);
        defaultSpecification = mock(Specification.class);
        when(defaultSpecification.CanCreateUser(any())).thenReturn(true);
        when(defaultSpecification.CanViewAllUsers(any())).thenReturn(true);
        defaultMailerClient = mock(MailerClient.class);
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

        userManager = new UserManager(userFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultSpecification);

        userManager.resetPassword(testUsername);

        verify(user).setPasswordHash(hashHelper.hashPassword(newPassword));
        verify(user).setIsPasswordResetRequired(true);
    }

    @Test(expected = UnauthorizedException.class)
    public void getAllObeysSpecification() throws UnauthorizedException {
        Specification spec = mock(Specification.class);
        when(spec.CanViewAllUsers(any(User.class))).thenReturn(false);
        HashHelper hashHelper = mock(HashHelper.class);
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(defaultUserFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, spec);
        sut.getAllUsers(1l);
    }

    @Test
    public void getAllReturnsUsers() throws UnauthorizedException {
        List<User> users = new ArrayList<User>();
        users.add(new User("müller", "email", "hash", true, 5));
        users.add(new User("michi", "email", "hash", true, 5));
        UserFinder userFinder = mock(UserFinder.class);
        when(userFinder.all()).thenReturn(users);
        HashHelper hashHelper = mock(HashHelper.class);
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(userFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultSpecification);
        List<User> result = sut.getAllUsers(1l);
        assertEquals(result.size(), 2);
    }

    @Test(expected = UnauthorizedException.class)
    public void createUserObeysSpecification() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException, UsernameCannotBeAdmin {
        Specification spec = mock(Specification.class);
        when(spec.CanCreateUser(any(User.class))).thenReturn(false);
        HashHelper hashHelper = mock(HashHelper.class);
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(defaultUserFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, spec);
        sut.createUser(1l, "klaus", "test@test.de", 5);
    }

    @Test(expected = UsernameAlreadyExistsException.class)
    public void createUserUsernameHasToBeUnique() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException, UsernameCannotBeAdmin {
        UserFinder userFinder = mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);
        User klausUser = mock(User.class);
        when(userFinder.byName("klaus")).thenReturn(Optional.of(klausUser));
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(userFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultSpecification);
        sut.createUser(1l, "klaus", "test@test.de", 5);
    }

    @Test(expected = EmailAlreadyExistsException.class)
    public void createUserEmailHasToBeUnique() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException, UsernameCannotBeAdmin {
        UserFinder userFinder = mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);
        User klausUser = mock(User.class);
        when(userFinder.byEmail(eq("test@test.de"), any())).thenReturn(Optional.of(klausUser));
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(userFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultSpecification);
        sut.createUser(1l, "klaus", "test@test.de", 5);
    }

    @Test
    public void createUserIssuesAdd() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException, UsernameCannotBeAdmin {
        HashHelper hashHelper = mock(HashHelper.class);
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        EbeanServer server = mock(EbeanServer.class);
        when(server.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));
        UserManager sut = new UserManager(defaultUserFinder, passwordGenerator, defaultMailerClient, hashHelper, server, defaultSpecification);

        ArgumentCaptor<User> argumentCaptor = ArgumentCaptor.forClass(User.class);
        sut.createUser(1l, "klaus", "test@test.de", 5);
        verify(server).save(argumentCaptor.capture());
        User addedUser = argumentCaptor.getValue();
        assertEquals(addedUser.getUsername(), "klaus");
        assertEquals(addedUser.getEmail(), "test@test.de");
        assertEquals(addedUser.getQuotaLimit(), 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChangePasswordWithNullInput(){

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserFinder userFinder= mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);

        userManager = new UserManager(userFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultSpecification);

        userManager.resetPassword(null);
    }

    @Test
    public void testDeleteUser() throws UnauthorizedException {

        UserFinder userFinder = mock(UserFinder.class);

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        HashHelper hashHelper = mock(HashHelper.class);

        userManager = new UserManager(userFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, new Specification());

        Long adminUserId = 0l;
        when(userFinder.byIdOptional(adminUserId)).thenReturn(Optional.of(adminUser));

        Long userToBeDeletedId = 10l;

        User userToBeDeleted = mock(User.class);
        when(userToBeDeleted.getUserId()).thenReturn(userToBeDeletedId);
        when(userFinder.byIdOptional(userToBeDeletedId)).thenReturn(Optional.of(userToBeDeleted));


        userManager.deleteUser(adminUserId, userToBeDeletedId);

        verify(defaultServer,times(1)).delete(userToBeDeleted);
    }

    @Test(expected = UnauthorizedException.class)
    public void testDeleteUserWithUnauthorizedUser() throws UnauthorizedException {

        UserFinder userFinder = mock(UserFinder.class);

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        HashHelper hashHelper = mock(HashHelper.class);

        userManager = new UserManager(userFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, new Specification());

        Long unauthorizedUserId = 0l;
        when(userFinder.byIdOptional(unauthorizedUserId)).thenReturn(Optional.of(mock(User.class)));

        Long userToBeDeletedId = 10l;

        User userToBeDeleted = mock(User.class);
        when(userToBeDeleted.getUserId()).thenReturn(userToBeDeletedId);
        when(userFinder.byIdOptional(userToBeDeletedId)).thenReturn(Optional.of(userToBeDeleted));

        userManager.deleteUser(unauthorizedUserId, userToBeDeletedId);

        verify(defaultServer, never()).delete(userToBeDeleted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteUserWithNullInput1() throws UnauthorizedException {

        UserFinder userFinder = mock(UserFinder.class);

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        HashHelper hashHelper = mock(HashHelper.class);

        userManager = new UserManager(userFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultSpecification);

        Long userToBeDeletedId = 10l;
        User userToBeDeleted = mock(User.class);
        when(userToBeDeleted.getUserId()).thenReturn(userToBeDeletedId);
        when(userFinder.byId(userToBeDeletedId)).thenReturn(userToBeDeleted);

        userManager.deleteUser(null, userToBeDeletedId);

        verify(defaultServer, never()).delete(userToBeDeleted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteUserWithNullInput2() throws UnauthorizedException {

        UserFinder userFinder = mock(UserFinder.class);

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        HashHelper hashHelper = mock(HashHelper.class);

        userManager = new UserManager(userFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultSpecification);

        Long adminUserId = 0l;
        when(userFinder.byId(adminUserId)).thenReturn(adminUser);

        userManager.deleteUser(adminUserId, null);

        verify(defaultServer, never()).delete(null);
    }
}
