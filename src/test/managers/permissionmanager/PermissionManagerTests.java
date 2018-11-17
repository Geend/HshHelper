package managers.permissionmanager;

import dtos.EditGroupPermissionDto;
import dtos.EditUserPermissionDto;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import io.ebean.EbeanServer;
import models.GroupPermission;
import models.PermissionLevel;
import models.User;
import models.UserPermission;
import models.finders.*;
import org.junit.Before;
import org.junit.Test;
import policyenforcement.Policy;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
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
        when(defaultPolicy.CanEditUserPermission(any(User.class), any(UserPermission.class))).thenReturn(true);
        when(defaultPolicy.CanEditGroupPermission(any(User.class), any(GroupPermission.class))).thenReturn(true);

        when(defaultUserFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(User.class)));
        when(defaultGroupPermissionFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(GroupPermission.class)));
        when(defaultUserPermissionFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(UserPermission.class)));
    }

    @Test(expected = UnauthorizedException.class)
    public void ShowEditUserPermissionIsAuthorizesTest() throws InvalidDataException, UnauthorizedException, InvalidArgumentException {
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

    @Test
    public void ShowEditUserPermissionDataTest() throws InvalidDataException, UnauthorizedException, InvalidArgumentException {
        UserPermissionFinder userFinder = mock(UserPermissionFinder.class);
        UserPermission p = mock(UserPermission.class);
        when(p.getCanRead()).thenReturn(true);
        when(p.getCanWrite()).thenReturn(false);
        when(p.getUserPermissionId()).thenReturn(55l);
        when(userFinder.byIdOptional(0l)).thenReturn(Optional.of(p));
        PermissionManager permissionManager = new PermissionManager(
                userFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                this.defaultPolicy);
        EditUserPermissionDto result = permissionManager.getUserPermissionForEdit(0l, 0l);
        assertEquals((long)result.getUserPermissionId(), 55l);
        assertEquals(result.getPermissionLevel(), PermissionLevel.READ);
    }

    @Test
    public void ShowEditGroupPermissionDataTest() throws InvalidDataException, UnauthorizedException, InvalidArgumentException {
        GroupPermissionFinder groupFinder = mock(GroupPermissionFinder.class);
        GroupPermission p = mock(GroupPermission.class);
        when(p.getCanRead()).thenReturn(true);
        when(p.getCanWrite()).thenReturn(false);
        when(p.getGroupPermissionId()).thenReturn(55l);
        when(groupFinder.byIdOptional(0l)).thenReturn(Optional.of(p));
        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                groupFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                this.defaultPolicy);
        EditGroupPermissionDto result = permissionManager.getGroupPermissionForEdit(0l, 0l);
        assertEquals((long)result.getGroupPermissionId(), 55l);
        assertEquals(result.getPermissionLevel(), PermissionLevel.READ);
    }


    @Test(expected = UnauthorizedException.class)
    public void ShowEditGroupPermissionIsAuthorizesTest() throws InvalidDataException, UnauthorizedException, InvalidArgumentException {
        Policy spec = mock(Policy.class);
        when(spec.CanEditGroupPermission(any(User.class), any(GroupPermission.class))).thenReturn(false);
        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                spec);
        permissionManager.getGroupPermissionForEdit(0l, 0l);
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
