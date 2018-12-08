package managers.usermanager;

import extension.*;
import io.ebean.EbeanServer;
import managers.UnauthorizedException;
import models.factories.UserFactory;
import models.finders.GroupFinder;
import models.finders.UserFinder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import policyenforcement.Policy;
import policyenforcement.session.SessionManager;
import twofactorauth.TwoFactorAuthService;

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
        UserFinder userFinder = mock(UserFinder.class);
        HashHelper hashHelper = mock(HashHelper.class);
        GroupFinder groupFinder = mock(GroupFinder.class);
        EbeanServer defaultServer = mock(EbeanServer.class);
        Policy spec = mock(Policy.class);
        SessionManager sessionManager = mock(SessionManager.class);
        when(spec.canCreateUser()).thenReturn(true);
        when(sessionManager.currentPolicy()).thenReturn(spec);
        when(userFinder.byName(any())).thenReturn(Optional.empty());
        PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
        UserFactory userFactory = mock(UserFactory.class);
        CredentialUtility credentialUtility = mock(CredentialUtility.class);
        WeakPasswords weakPasswords = mock(WeakPasswords.class);
        TwoFactorAuthService twoFactorAuthService = mock(TwoFactorAuthService.class);

        UserManager sut = new UserManager(
                userFinder,
                groupFinder,
                passwordGenerator,
                hashHelper,
                defaultServer,
                sessionManager,
                userFactory,
                credentialUtility,
                weakPasswords,
                twoFactorAuthService);
        sut.createUser(this.username, "test@test.de", 5l);
    }
}
