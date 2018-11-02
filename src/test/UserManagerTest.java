import domainlogic.usermanager.UserManager;
import extension.HashHelper;
import extension.PasswordGenerator;
import models.User;
import models.finders.UserFinder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import play.libs.mailer.MailerClient;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserManagerTest {


    UserManager userManager;




    MailerClient mailerClient;

    @Before
    public void init() {
        mailerClient = mock(MailerClient.class);
    }


    @Test
    public void testChangePassword() {

        String testUsername = "test";
        String newPassword= "0123456789";
        String newPasswordHash = "abcdefg";


        HashHelper hashHelper = mock(HashHelper.class);
        when(hashHelper.hashPassword(newPassword)).thenReturn(newPasswordHash);

        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        when(passwordGenerator.generatePassword(10)).thenReturn(newPassword);


        User user = mock(User.class);
        when(user.getUsername()).thenReturn(testUsername);

        UserFinder userFinder= mock(UserFinder.class);
        when(userFinder.byName(testUsername)).thenReturn(Optional.of(user));

        userManager = new UserManager(userFinder, passwordGenerator, mailerClient, hashHelper);

        userManager.resetPassword(testUsername);

        verify(user).setPasswordHash(hashHelper.hashPassword(newPassword));
        verify(user).setIsPasswordResetRequired(true);
    }
}
