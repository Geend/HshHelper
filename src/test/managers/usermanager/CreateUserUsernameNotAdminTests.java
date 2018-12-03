package managers.usermanager;

import extension.CredentialManager;
import extension.HashHelper;
import extension.PasswordGenerator;
import extension.RecaptchaHelper;
import io.ebean.EbeanServer;
import managers.UnauthorizedException;
import models.factories.UserFactory;
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
        RecaptchaHelper recaptchaHelper = mock(RecaptchaHelper.class);
        when(spec.canCreateUser()).thenReturn(true);
        when(sessionManager.currentPolicy()).thenReturn(spec);
        when(userFinder.byName(any())).thenReturn(Optional.empty());
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserFactory userFactory = mock(UserFactory.class);
        CredentialManager credentialManager = mock(CredentialManager.class);

        UserManager sut = new UserManager(userFinder, groupFinder, passwordGenerator, mailer, hashHelper, defaultServer, sessionManager, recaptchaHelper, userFactory, credentialManager);
        sut.createUser(this.username, "test@test.de", 5l);
    }
}
