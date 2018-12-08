package managers.usermanager;

import extension.*;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import models.User;
import models.factories.UserFactory;
import models.finders.GroupFinder;
import models.finders.UserFinder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import policyenforcement.Policy;
import policyenforcement.session.SessionManager;
import twofactorauth.TwoFactorAuthService;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserManagerTest {

    private User nonAdminUser;
    private User adminUser;
    private UserManager userManager;
    private UserFinder defaultUserFinder;
    private GroupFinder defaultGroupFinder;
    private EbeanServer defaultServer;
    private Policy defaultPolicy;
    private SessionManager defaultSessionManager;
    private UserFactory defaultUserFactory;
    private CredentialManager defaultCredentialManager;
    private WeakPasswords defaultWeakPasswords;
    private TwoFactorAuthService defaultTwoFactorAuthService;
    private PasswordGenerator defaultPasswordGenerator;
    private HashHelper defaultHashHelper;

    @Before
    public void init() {
        defaultHashHelper = mock(HashHelper.class);
        defaultPasswordGenerator = mock(PasswordGenerator.class);
        defaultGroupFinder = mock(GroupFinder.class);
        defaultUserFinder = mock(UserFinder.class);
        defaultPolicy = mock(Policy.class);
        defaultSessionManager = mock(SessionManager.class);
        when(defaultPolicy.canCreateUser()).thenReturn(true);
        when(defaultPolicy.canViewAllUsers()).thenReturn(true);
        when(defaultSessionManager.currentPolicy()).thenReturn(defaultPolicy);
        adminUser = mock(User.class);
        nonAdminUser = mock(User.class);
        defaultServer = mock(EbeanServer.class);
        when(defaultServer.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));
        when(adminUser.isAdmin()).thenReturn(true);
        when(nonAdminUser.isAdmin()).thenReturn(false);
        when(defaultSessionManager.currentUser()).thenReturn(adminUser);
        defaultUserFactory = mock(UserFactory.class);
        defaultCredentialManager = mock(CredentialManager.class);
        defaultWeakPasswords = mock(WeakPasswords.class);
        defaultTwoFactorAuthService = mock(TwoFactorAuthService.class);
    }


    @Test(expected = UnauthorizedException.class)
    public void getAllObeysSpecification() throws UnauthorizedException {
        Policy spec = mock(Policy.class);
        when(spec.canViewAllUsers()).thenReturn(false);
        when(defaultSessionManager.currentPolicy()).thenReturn(spec);

        HashHelper hashHelper = mock(HashHelper.class);
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(
                defaultUserFinder,
                defaultGroupFinder,
                passwordGenerator,
                hashHelper,
                defaultServer,
                defaultSessionManager,
                defaultUserFactory,
                defaultCredentialManager,
                defaultWeakPasswords,
                defaultTwoFactorAuthService);
        sut.getAllUsers();
    }

    @Test
    public void activateTwoFactorAuthTest() throws Invalid2FATokenException, GeneralSecurityException {
        User user = mock(User.class);
        EbeanServer server = mock(EbeanServer.class);
        SessionManager sessionManager = mock(SessionManager.class);
        when(sessionManager.currentUser()).thenReturn(user);
        TwoFactorAuthService authService = mock(TwoFactorAuthService.class);
        boolean exceptionOccured = false;
        UserManager sut = new UserManager(
                defaultUserFinder,
                defaultGroupFinder,
                defaultPasswordGenerator,
                defaultHashHelper,
                server,
                sessionManager,
                defaultUserFactory,
                defaultCredentialManager,
                defaultWeakPasswords,
                authService);

        // invalid secret
        try {
            sut.activateTwoFactorAuth("invalid", "234567");
        } catch(Invalid2FATokenException e) {
            exceptionOccured = true;
        }
        assertTrue(exceptionOccured);
        exceptionOccured = false;

        // invalid token
        try {
            sut.activateTwoFactorAuth("ORT4CT7FHMPJB6X2", "invalidtoken");
        } catch(Invalid2FATokenException e) {
            exceptionOccured = true;
        }
        assertTrue(exceptionOccured);
        when(authService.validateCurrentNumber(anyString(), anyInt(), anyInt())).thenReturn(true);
        sut.activateTwoFactorAuth("ORT4CT7FHMPJB6X2", "765312");
        verify(user).setTwoFactorAuthSecret("ORT4CT7FHMPJB6X2");
        verify(server).save(user);
    }

    @Test
    public void getAllReturnsUsers() throws UnauthorizedException {
        List<User> users = new ArrayList<User>();
        users.add(new User("müller", "email", "hash", true, 5l));
        users.add(new User("michi", "email", "hash", true, 5l));
        UserFinder userFinder = mock(UserFinder.class);
        when(userFinder.all()).thenReturn(users);
        HashHelper hashHelper = mock(HashHelper.class);
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(
                userFinder,
                defaultGroupFinder,
                passwordGenerator,
                hashHelper,
                defaultServer,
                defaultSessionManager,
                defaultUserFactory,
                defaultCredentialManager,
                defaultWeakPasswords,
                defaultTwoFactorAuthService);
        List<User> result = sut.getAllUsers();
        assertEquals(result.size(), 2);
    }

    @Test(expected = UnauthorizedException.class)
    public void createUserObeysSpecification() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException, UsernameCannotBeAdmin {
        Policy spec = mock(Policy.class);
        when(spec.canCreateUser()).thenReturn(false);
        when(defaultSessionManager.currentPolicy()).thenReturn(spec);
        HashHelper hashHelper = mock(HashHelper.class);
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(
                defaultUserFinder,
                defaultGroupFinder,
                passwordGenerator,
                hashHelper,
                defaultServer,
                defaultSessionManager,
                defaultUserFactory,
                defaultCredentialManager,
                defaultWeakPasswords,
                defaultTwoFactorAuthService);
        sut.createUser( "klaus", "test@test.de", 5l);
    }

    @Test(expected = UsernameAlreadyExistsException.class)
    public void createUserUsernameHasToBeUnique() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException, UsernameCannotBeAdmin {
        UserFinder userFinder = mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);
        User klausUser = mock(User.class);
        when(userFinder.byName("klaus")).thenReturn(Optional.of(klausUser));
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(
                userFinder,
                defaultGroupFinder,
                passwordGenerator,
                hashHelper,
                defaultServer,
                defaultSessionManager,
                defaultUserFactory,
                defaultCredentialManager,
                defaultWeakPasswords,
                defaultTwoFactorAuthService);
        sut.createUser("klaus", "test@test.de", 5l);
    }

    @Test(expected = EmailAlreadyExistsException.class)
    public void createUserEmailHasToBeUnique() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException, UsernameCannotBeAdmin {
        UserFinder userFinder = mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);
        User klausUser = mock(User.class);
        when(userFinder.byEmail(eq("test@test.de"), any())).thenReturn(Optional.of(klausUser));
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(
                userFinder,
                defaultGroupFinder,
                passwordGenerator,
                hashHelper,
                defaultServer,
                defaultSessionManager,
                defaultUserFactory,
                defaultCredentialManager,
                defaultWeakPasswords,
                defaultTwoFactorAuthService);
        sut.createUser( "klaus", "test@test.de", 5l);
    }

    @Test
    public void createUserIssuesAdd() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException, UsernameCannotBeAdmin {
        HashHelper hashHelper = mock(HashHelper.class);
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        when(passwordGenerator.generatePassword(anyInt())).thenReturn("sss");
        EbeanServer server = mock(EbeanServer.class);
        when(server.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));
        when(defaultUserFactory.CreateUser(any(String.class), any(String.class), any(String.class), any(Boolean.class), any(Long.class))).thenReturn(
                new User("klaus", "test@test.de", "", true,5l)
        );
        UserManager sut = new UserManager(
                defaultUserFinder,
                defaultGroupFinder,
                passwordGenerator,
                hashHelper,
                server,
                defaultSessionManager,
                defaultUserFactory,
                defaultCredentialManager,
                defaultWeakPasswords,
                defaultTwoFactorAuthService);

        ArgumentCaptor<User> argumentCaptor = ArgumentCaptor.forClass(User.class);
        sut.createUser( "klaus", "test@test.de", 5L);
        verify(server).save(argumentCaptor.capture());
        User addedUser = argumentCaptor.getValue();
        assertEquals(addedUser.getUsername(), "klaus");
        assertEquals(addedUser.getEmail(), "test@test.de");
        assertEquals(addedUser.getQuotaLimit(), new Long(5L));
    }

    @Test
    public void testDeleteUser() throws UnauthorizedException, InvalidArgumentException {

        UserFinder userFinder = mock(UserFinder.class);

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        HashHelper hashHelper = mock(HashHelper.class);

        when(defaultPolicy.canDeleteUser(any())).thenReturn(true);

        userManager = new UserManager(
                userFinder,
                defaultGroupFinder,
                passwordGenerator,
                hashHelper,
                defaultServer,
                defaultSessionManager,
                defaultUserFactory,
                defaultCredentialManager,
                defaultWeakPasswords,
                defaultTwoFactorAuthService);

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
        when(sessionManager.currentUser()).thenReturn(nonAdminUser);
        when(sessionManager.currentPolicy()).thenReturn(Policy.ForUser(nonAdminUser));

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        HashHelper hashHelper = mock(HashHelper.class);

        userManager = new UserManager(
                userFinder,
                defaultGroupFinder,
                passwordGenerator,
                hashHelper,
                defaultServer,
                sessionManager,
                defaultUserFactory,
                defaultCredentialManager,
                defaultWeakPasswords,
                defaultTwoFactorAuthService);

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

        userManager = new UserManager(
                userFinder,
                defaultGroupFinder,
                passwordGenerator,
                hashHelper,
                defaultServer,
                defaultSessionManager,
                defaultUserFactory,
                defaultCredentialManager,
                defaultWeakPasswords,
                defaultTwoFactorAuthService);

        Long adminUserId = 0l;
        when(userFinder.byId(adminUserId)).thenReturn(adminUser);

        userManager.deleteUser(null);

        verify(defaultServer, never()).delete(null);
    }
}
