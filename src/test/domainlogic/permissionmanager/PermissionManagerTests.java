package domainlogic.permissionmanager;

import domainlogic.InvalidArgumentException;
import domainlogic.UnauthorizedException;
import io.ebean.EbeanServer;
import models.File;
import models.GroupPermission;
import models.User;
import models.finders.*;
import org.junit.Before;
import org.junit.Test;
import policy.Specification;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PermissionManagerTests {
    private UserPermissionFinder defaultUserPermissionFinder;
    private GroupPermissionFinder defaultGroupPermissionFinder;
    private EbeanServer defaultEbeanServer;
    private FileFinder defaultFileFinder;
    private GroupFinder defaultGroupFinder;
    private UserFinder defaultUserFinder;
    private Specification defaultSpecification;

    @Before
    public void init() {
        this.defaultUserFinder = mock(UserFinder.class);
        this.defaultSpecification = mock(Specification.class);
        this.defaultGroupFinder = mock(GroupFinder.class);
        this.defaultFileFinder = mock(FileFinder.class);
        this.defaultEbeanServer = mock(EbeanServer.class);
        this.defaultGroupPermissionFinder = mock(GroupPermissionFinder.class);
        this.defaultUserPermissionFinder = mock(UserPermissionFinder.class);

        when(defaultUserFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(User.class)));
        when(defaultGroupPermissionFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(GroupPermission.class)));
    }

    @Test(expected = UnauthorizedException.class)
    public void DeleteGroupPermissionIsAuthorized() throws UnauthorizedException, InvalidArgumentException {
        Specification spec = mock(Specification.class);
        when(spec.CanDeleteGroupPermission(any(User.class), any(GroupPermission.class))).thenReturn(false);
        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                this.defaultSpecification);
        permissionManager.deleteGroupPermission(0l, 0l);
    }
}
