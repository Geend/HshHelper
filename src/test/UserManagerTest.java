import domainlogic.usermanager.UserManager;
import models.User;
import models.finders.UserFinder;
import org.junit.Before;
import org.junit.Test;
import play.libs.mailer.MailerClient;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserManagerTest {


    UserManager userManager;


    UserFinder userFinder;


    MailerClient mailerClient;

    @Before
    public void init() {

        mailerClient = mock(MailerClient.class);


        userFinder = mock(UserFinder.class);

        User user = mock(User.class);
        when(user.getUsername()).thenReturn("test");
        when(userFinder.byIdOptional(0l)).thenReturn(Optional.of(user));

        Optional<User> testUser = userFinder.byIdOptional(0l);
       // userManager = new UserManager(userFinder, )

    }


    @Test
    public void testCreateUser() {

      //  userManager.createUser("testuser", "test@example.org", 1234);

    }
}
