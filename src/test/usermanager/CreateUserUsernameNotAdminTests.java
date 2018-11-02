package usermanager;

import domainlogic.UnauthorizedException;
import domainlogic.usermanager.EmailAlreadyExistsException;
import domainlogic.usermanager.UserManager;
import domainlogic.usermanager.UsernameAlreadyExistsException;
import domainlogic.usermanager.UsernameCannotBeAdmin;
import extension.HashHelper;
import extension.PasswordGenerator;
import io.ebean.EbeanServer;
import models.finders.UserFinder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import play.libs.mailer.MailerClient;
import policy.Specification;

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
        EbeanServer defaultServer = mock(EbeanServer.class);
        Specification spec = mock(Specification.class);
        when(spec.CanCreateUser(any())).thenReturn(true);
        when(userFinder.byName(any())).thenReturn(Optional.empty());
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserManager sut = new UserManager(userFinder, passwordGenerator, mailer, hashHelper, defaultServer, spec);
        sut.createUser(1l, this.username, "test@test.de", 5);
    }
}
