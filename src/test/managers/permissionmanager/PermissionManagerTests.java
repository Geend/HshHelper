package managers.permissionmanager;

import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import io.ebean.EbeanServer;
import models.GroupPermission;
import models.User;
import models.UserPermission;
import models.finders.*;
import org.junit.Before;
import org.junit.Test;
import policyenforcement.Policy;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PermissionManagerTests {
    private UserPermissionFinder defaultUserPermissionFinder;
    private GroupPermissionFinder defaultGroupPermissionFinder;
    private EbeanServer defaultEbeanServer;
    private FileFinder defaultFileFinder;
    private GroupFinder defaultGroupFinder;
    private UserFinder defaultUserFinder;
    private Policy defaultPolicy;

    @Before
    public void init() {
        this.defaultUserFinder = mock(UserFinder.class);
        this.defaultPolicy = mock(Policy.class);
        this.defaultGroupFinder = mock(GroupFinder.class);
        this.defaultFileFinder = mock(FileFinder.class);
        this.defaultEbeanServer = mock(EbeanServer.class);
        this.defaultGroupPermissionFinder = mock(GroupPermissionFinder.class);
        this.defaultUserPermissionFinder = mock(UserPermissionFinder.class);
        when(defaultPolicy.CanDeleteGroupPermission(any(User.class), any(GroupPermission.class))).thenReturn(true);
        when(defaultPolicy.CanDeleteUserPermission(any(User.class), any(UserPermission.class))).thenReturn(true);

        when(defaultUserFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(User.class)));
        when(defaultGroupPermissionFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(GroupPermission.class)));
        when(defaultUserPermissionFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(UserPermission.class)));
    }

    @Test(expected = UnauthorizedException.class)
    public void ShowEditEditPermissionIsAuthorizes() throws InvalidDataException, UnauthorizedException, InvalidArgumentException {
        Policy spec = mock(Policy.class);
        when(spec.CanEditUserPermission(any(User.class), any(UserPermission.class))).thenReturn(false);
        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                spec);
        permissionManager.getUserPermissionForEdit(0l, 0l);
    }

    @Test(expected = UnauthorizedException.class)
    public void DeleteGroupPermissionIsAuthorizedTest() throws UnauthorizedException, InvalidArgumentException {
        Policy spec = mock(Policy.class);
        when(spec.CanDeleteGroupPermission(any(User.class), any(GroupPermission.class))).thenReturn(false);
        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                spec);
        permissionManager.deleteGroupPermission(0l, 0l);
    }

    @Test()
    public void DeleteGroupPermissionTest() throws UnauthorizedException, InvalidArgumentException {
        GroupPermission permission = mock(GroupPermission.class);
        GroupPermissionFinder groupPermissionFinder = mock(GroupPermissionFinder.class);
        when(groupPermissionFinder.byIdOptional(0l)).thenReturn(Optional.of(permission));
        EbeanServer s = mock(EbeanServer.class);
        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                groupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                s,
                this.defaultPolicy);
        permissionManager.deleteGroupPermission(0l, 0l);

        verify(s).delete(permission);
    }

    @Test()
    public void DeleteUserPermissionTest() throws UnauthorizedException, InvalidArgumentException {
        UserPermission permission = mock(UserPermission.class);
        UserPermissionFinder userPermissionFinder = mock(UserPermissionFinder.class);
        when(userPermissionFinder.byIdOptional(0l)).thenReturn(Optional.of(permission));

        EbeanServer s = mock(EbeanServer.class);
        PermissionManager permissionManager = new PermissionManager(
                userPermissionFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                s,
                this.defaultPolicy);
        permissionManager.deleteUserPermission(0l, 0l);

        verify(s).delete(permission);
    }

    @Test(expected = UnauthorizedException.class)
    public void DeleteUserPermissionIsAuthorizedTest() throws UnauthorizedException, InvalidArgumentException {
        Policy spec = mock(Policy.class);
        when(spec.CanDeleteUserPermission(any(User.class), any(UserPermission.class))).thenReturn(false);
        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                spec);
        permissionManager.deleteUserPermission(0l, 0l);
    }
}