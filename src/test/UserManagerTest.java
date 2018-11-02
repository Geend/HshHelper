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
import org.junit.Before;
import org.junit.Test;
import play.libs.mailer.MailerClient;
import policy.Specification;

import java.util.Optional;

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
    public void createUserObeysSpecification() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException {
        Specification spec = mock(Specification.class);
        when(spec.CanCreateUser(any(User.class))).thenReturn(false);
        HashHelper hashHelper = mock(HashHelper.class);
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(defaultUserFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultSpecification);
        sut.createUser(1l, "klaus", "test@test.de", 5);
    }

    @Test(expected = UsernameAlreadyExistsException.class)
    public void createUsernameHasToBeUnique() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException {
        UserFinder userFinder = mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);
        User klausUser = mock(User.class);
        when(userFinder.byName("klaus")).thenReturn(Optional.of(klausUser));
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(userFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultSpecification);
        sut.createUser(1l, "klaus", "test@test.de", 5);
    }

    @Test(expected = EmailAlreadyExistsException.class)
    public void createUserEmailHasToBeUnique() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException {
        UserFinder userFinder = mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);
        User klausUser = mock(User.class);
        when(userFinder.byEmail("test@test.de")).thenReturn(Optional.of(klausUser));
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(userFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultSpecification);
        sut.createUser(1l, "klaus", "test@test.de", 5);
    }

    @Test
    public void testChangePasswordWithNullInput(){

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserFinder userFinder= mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);

        userManager = new UserManager(userFinder, passwordGenerator, defaultMailerClient, hashHelper, defaultServer, defaultSpecification);

        userManager.resetPassword(null);
    }
}
