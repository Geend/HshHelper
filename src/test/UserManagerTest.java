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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserManagerTest {


    UserManager userManager;
    User adminUser;
    MailerClient mailerClient;
    EbeanServer defaultServer;

    @Before
    public void init() {
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

        userManager = new UserManager(userFinder, passwordGenerator, mailerClient, hashHelper, defaultServer);

        userManager.resetPassword(testUsername);

        verify(user).setPasswordHash(hashHelper.hashPassword(newPassword));
        verify(user).setIsPasswordResetRequired(true);
    }

    @Test(expected = UnauthorizedException.class)
    public void onlyAdminCanCreateUser() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException {
        UserFinder userFinder = mock(UserFinder.class);
        User normalUser = mock(User.class);
        when(normalUser.isAdmin()).thenReturn(false);
        when(userFinder.byId(1l)).thenReturn(normalUser);
        HashHelper hashHelper = mock(HashHelper.class);
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(userFinder, passwordGenerator, mailerClient, hashHelper, defaultServer);
        sut.createUser(1l, "klaus", "test@test.de", 5);
    }

    @Test
    public void testChangePasswordWithNullInput(){

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserFinder userFinder= mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);

        userManager = new UserManager(userFinder, passwordGenerator, mailerClient, hashHelper, defaultServer);

        userManager.resetPassword(null);
    }
}
