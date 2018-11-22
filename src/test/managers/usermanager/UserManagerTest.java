package managers.usermanager;

import extension.RecaptchaHelper;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import extension.HashHelper;
import extension.PasswordGenerator;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import managers.loginmanager.CaptchaRequiredException;
import models.User;
import models.finders.GroupFinder;
import models.finders.UserFinder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.libs.mailer.MailerClient;
import play.test.Helpers;
import policyenforcement.Policy;
import policyenforcement.session.SessionManager;

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
    GroupFinder defaultGroupFinder;
    MailerClient defaultMailerClient;
    EbeanServer defaultServer;
    Policy defaultPolicy;
    SessionManager defaultSessionManager;
    private RecaptchaHelper recaptchaHelper;

    @Before
    public void init() {
        defaultGroupFinder = mock(GroupFinder.class);
        defaultUserFinder = mock(UserFinder.class);
        defaultPolicy = mock(Policy.class);
        when(defaultPolicy.CanCreateUser(any())).thenReturn(true);
        when(defaultPolicy.CanViewAllUsers(any())).thenReturn(true);
        defaultMailerClient = mock(MailerClient.class);
        adminUser = mock(User.class);
        defaultServer = mock(EbeanServer.class);
        when(defaultServer.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));
        when(adminUser.isAdmin()).thenReturn(true);
        defaultSessionManager = mock(SessionManager.class);
        recaptchaHelper = mock(RecaptchaHelper.class);
        when(defaultSessionManager.currentUser()).thenReturn(adminUser);
    }

    @Test
    public void testChangePassword() throws InvalidArgumentException, CaptchaRequiredException {

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

        when(recaptchaHelper.IsValidResponse(anyString(), anyString())).thenReturn(true);

        userManager = new UserManager(userFinder, defaultGroupFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultPolicy, defaultSessionManager, recaptchaHelper);

        userManager.resetPassword(testUsername, "", Helpers.fakeRequest().build());

        verify(user).setPasswordHash(hashHelper.hashPassword(newPassword));
        verify(user).setIsPasswordResetRequired(true);
    }

    @Test(expected = CaptchaRequiredException.class)
    public void resetPasswordWillNotWorkWithoutCaptcha() throws CaptchaRequiredException, InvalidArgumentException {
        HashHelper hashHelper = mock(HashHelper.class);
        when(hashHelper.hashPassword("")).thenReturn("");

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        when(passwordGenerator.generatePassword(0)).thenReturn("");

        User user = mock(User.class);
        UserFinder userFinder = mock(UserFinder.class);
        when(userFinder.byName("")).thenReturn(Optional.of(user));

        userManager = new UserManager(userFinder, defaultGroupFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultPolicy, defaultSessionManager, recaptchaHelper);

        userManager.resetPassword("", "", Helpers.fakeRequest().build());
    }

    @Test(expected = UnauthorizedException.class)
    public void getAllObeysSpecification() throws UnauthorizedException {
        Policy spec = mock(Policy.class);
        when(spec.CanViewAllUsers(any(User.class))).thenReturn(false);
        HashHelper hashHelper = mock(HashHelper.class);
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(defaultUserFinder, defaultGroupFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, spec, defaultSessionManager, recaptchaHelper);
        sut.getAllUsers();
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
        UserManager sut = new UserManager(userFinder, defaultGroupFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultPolicy, defaultSessionManager, recaptchaHelper);
        List<User> result = sut.getAllUsers();
        assertEquals(result.size(), 2);
    }

    @Test(expected = UnauthorizedException.class)
    public void createUserObeysSpecification() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException, UsernameCannotBeAdmin {
        Policy spec = mock(Policy.class);
        when(spec.CanCreateUser(any(User.class))).thenReturn(false);
        HashHelper hashHelper = mock(HashHelper.class);
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(defaultUserFinder, defaultGroupFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, spec, defaultSessionManager, recaptchaHelper);
        sut.createUser( "klaus", "test@test.de", 5);
    }

    @Test(expected = UsernameAlreadyExistsException.class)
    public void createUserUsernameHasToBeUnique() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException, UsernameCannotBeAdmin {
        UserFinder userFinder = mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);
        User klausUser = mock(User.class);
        when(userFinder.byName("klaus")).thenReturn(Optional.of(klausUser));
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(userFinder, defaultGroupFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultPolicy, defaultSessionManager, recaptchaHelper);
        sut.createUser("klaus", "test@test.de", 5);
    }

    @Test(expected = EmailAlreadyExistsException.class)
    public void createUserEmailHasToBeUnique() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException, UsernameCannotBeAdmin {
        UserFinder userFinder = mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);
        User klausUser = mock(User.class);
        when(userFinder.byEmail(eq("test@test.de"), any())).thenReturn(Optional.of(klausUser));
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(userFinder, defaultGroupFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultPolicy, defaultSessionManager, recaptchaHelper);
        sut.createUser( "klaus", "test@test.de", 5);
    }

    @Test
    public void createUserIssuesAdd() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException, UsernameCannotBeAdmin {
        HashHelper hashHelper = mock(HashHelper.class);
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        EbeanServer server = mock(EbeanServer.class);
        when(server.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));
        UserManager sut = new UserManager(defaultUserFinder, defaultGroupFinder, passwordGenerator, defaultMailerClient, hashHelper, server, defaultPolicy, defaultSessionManager, recaptchaHelper);

        ArgumentCaptor<User> argumentCaptor = ArgumentCaptor.forClass(User.class);
        sut.createUser( "klaus", "test@test.de", 5);
        verify(server).save(argumentCaptor.capture());
        User addedUser = argumentCaptor.getValue();
        assertEquals(addedUser.getUsername(), "klaus");
        assertEquals(addedUser.getEmail(), "test@test.de");
        assertEquals(addedUser.getQuotaLimit(), 5);
    }

    @Test(expected = InvalidArgumentException.class)
    public void testChangePasswordWithNullInput() throws InvalidArgumentException, CaptchaRequiredException {

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserFinder userFinder= mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);

        userManager = new UserManager(userFinder, defaultGroupFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultPolicy, defaultSessionManager, recaptchaHelper);

        userManager.resetPassword(null, "", Helpers.fakeRequest().build());
    }

    @Test
    public void testDeleteUser() throws UnauthorizedException, InvalidArgumentException {

        UserFinder userFinder = mock(UserFinder.class);

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        HashHelper hashHelper = mock(HashHelper.class);

        userManager = new UserManager(userFinder, defaultGroupFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, new Policy(), defaultSessionManager, recaptchaHelper);

        Long adminUserId = 0l;
        when(userFinder.byIdOptional(adminUserId)).thenReturn(Optional.of(adminUser));

        Long userToBeDeletedId = 10l;

        User userToBeDeleted = mock(User.class);
        when(userToBeDeleted.getUserId()).thenReturn(userToBeDeletedId);
        when(userFinder.byIdOptional(userToBeDeletedId)).thenReturn(Optional.of(userToBeDeleted));


        userManager.deleteUser(userToBeDeletedId);

        verify(defaultServer,times(1)).delete(userToBeDeleted);
    }

    @Test(expected = UnauthorizedException.class)
    public void testDeleteUserWithUnauthorizedUser() throws UnauthorizedException, InvalidArgumentException {

        UserFinder userFinder = mock(UserFinder.class);

        SessionManager sessionManager = mock(SessionManager.class);
        when(sessionManager.currentUser()).thenReturn((mock(User.class)));

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        HashHelper hashHelper = mock(HashHelper.class);

        userManager = new UserManager(userFinder, defaultGroupFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, new Policy(), sessionManager, recaptchaHelper);

        Long userToBeDeletedId = 10l;

        User userToBeDeleted = mock(User.class);
        when(userToBeDeleted.getUserId()).thenReturn(userToBeDeletedId);
        when(userFinder.byIdOptional(userToBeDeletedId)).thenReturn(Optional.of(userToBeDeleted));

        userManager.deleteUser(userToBeDeletedId);

        verify(defaultServer, never()).delete(userToBeDeleted);
    }



    @Test(expected = InvalidArgumentException.class)
    public void testDeleteUserWithNullInput() throws UnauthorizedException, InvalidArgumentException {

        UserFinder userFinder = mock(UserFinder.class);

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        HashHelper hashHelper = mock(HashHelper.class);

        userManager = new UserManager(userFinder, defaultGroupFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultPolicy, defaultSessionManager, recaptchaHelper);

        Long adminUserId = 0l;
        when(userFinder.byId(adminUserId)).thenReturn(adminUser);

        userManager.deleteUser(null);

        verify(defaultServer, never()).delete(null);
    }
}
