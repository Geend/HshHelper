package managers.usermanager;

import managers.UnauthorizedException;
import extension.HashHelper;
import extension.PasswordGenerator;
import io.ebean.EbeanServer;
import models.finders.GroupFinder;
import models.finders.UserFinder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import play.libs.mailer.MailerClient;
import policyenforcement.Policy;
import policyenforcement.session.SessionManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class CreateUserUsernameNotAdminTests {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"admin"},
                {"Admin"},
                {"ADMIN"},
                {"admiN"},
        });
    }
    private String username;

    public CreateUserUsernameNotAdminTests(String username) {
        this.username = username;
    }

    @Test(expected = UsernameCannotBeAdmin.class)
    public void createUserUsernameCannotBeAdmin() throws EmailAlreadyExistsException, UnauthorizedException, UsernameAlreadyExistsException, UsernameCannotBeAdmin {
        MailerClient mailer = mock(MailerClient.class);
        UserFinder userFinder = mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);
        GroupFinder groupFinder = mock(GroupFinder.class);
        EbeanServer defaultServer = mock(EbeanServer.class);
        Policy spec = mock(Policy.class);
        SessionManager sessionManager = mock(SessionManager.class);
        when(spec.CanCreateUser(any())).thenReturn(true);
        when(userFinder.byName(any())).thenReturn(Optional.empty());
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(userFinder, groupFinder, passwordGenerator, mailer, hashHelper, defaultServer, spec, sessionManager);
        sut.createUser(this.username, "test@test.de", 5);
    }
}
