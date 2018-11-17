package managers.permissionmanager;

import dtos.EditGroupPermissionDto;
import dtos.EditUserPermissionDto;
import dtos.PermissionEntryDto;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import io.ebean.EbeanServer;
import models.*;
import models.finders.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import policyenforcement.Policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
        when(defaultPolicy.CanCreateUserPermission(any(File.class), any(User.class))).thenReturn(true);
        when(defaultPolicy.CanCreateGroupPermission(any(File.class), any(User.class), any(Group.class))).thenReturn(true);

        when(defaultUserFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(User.class)));
        when(defaultGroupPermissionFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(GroupPermission.class)));
        when(defaultUserPermissionFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(UserPermission.class)));
        when(defaultFileFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(File.class)));
    }

    @Test
    public void getAllGrantedPermissions() {
        ArrayList<File> userFiles = new ArrayList<>();
        File f = mock(File.class);
        when(f.getFileId()).thenReturn(55l);
        userFiles.add(f);
        FileFinder fileFinder = mock(FileFinder.class);
        when(fileFinder.getFilesByOwner(any(Long.class))).thenReturn(userFiles);

        Group g = mock(Group.class);
        User u = mock(User.class);
        GroupPermission gp = mock(GroupPermission.class);
        UserPermission up = mock(UserPermission.class);
        when(gp.getGroupPermissionId()).thenReturn(50l);
        when(up.getUserPermissionId()).thenReturn(100l);
        when(gp.getCanRead()).thenReturn(true);
        when(up.getCanWrite()).thenReturn(true);
        when(gp.getGroup()).thenReturn(g);
        when(up.getUser()).thenReturn(u);
        ArrayList<GroupPermission> groupPermissionsForFile = new ArrayList<>();
        groupPermissionsForFile.add(gp);
        ArrayList<UserPermission> userPermissionsForFile = new ArrayList<>();
        userPermissionsForFile.add(up);

        GroupPermissionFinder groupPermFinder = mock(GroupPermissionFinder.class);
        UserPermissionFinder userPermFinder = mock(UserPermissionFinder.class);
        when(groupPermFinder.findForFileId(55l)).thenReturn(groupPermissionsForFile);
        when(userPermFinder.findForFileId(55l)).thenReturn(userPermissionsForFile);

        PermissionManager permissionManager = new PermissionManager(
                userPermFinder,
                groupPermFinder,
                fileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                this.defaultPolicy);

        List<PermissionEntryDto> grantedPermissions = permissionManager.getAllGrantedPermissions(0l);
        assertEquals(grantedPermissions.size(), 2);
        assertTrue(grantedPermissions.stream().anyMatch(x -> x.getIsGroupPermission() && x.getGroupOrUserIdentifier() == 50l));
        assertTrue(grantedPermissions.stream().anyMatch(x -> !x.getIsGroupPermission() && x.getGroupOrUserIdentifier() == 100l));
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
    public void EditUserPermissionTest() throws UnauthorizedException, InvalidArgumentException {
        UserPermission up = mock(UserPermission.class);
        UserPermissionFinder upf = mock(UserPermissionFinder.class);
        when(upf.byIdOptional(0l)).thenReturn(Optional.of(up));
        EbeanServer s = mock(EbeanServer.class);
        PermissionManager permissionManager = new PermissionManager(
                upf,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                s,
                this.defaultPolicy);
        permissionManager.editUserPermission(0l, 0l, PermissionLevel.WRITE);
        verify(up).setCanRead(false);
        verify(up).setCanWrite(true);
        verify(s).save(up);
    }

    @Test
    public void EditGroupPermissionTest() throws UnauthorizedException, InvalidArgumentException {
        GroupPermission gp = mock(GroupPermission.class);
        GroupPermissionFinder gpf = mock(GroupPermissionFinder.class);
        when(gpf.byIdOptional(0l)).thenReturn(Optional.of(gp));
        EbeanServer s = mock(EbeanServer.class);
        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                gpf,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                s,
                this.defaultPolicy);
        permissionManager.editGroupPermission(0l, 0l, PermissionLevel.READ);
        verify(gp).setCanRead(true);
        verify(gp).setCanWrite(false);
        verify(s).save(gp);
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

    /*
        Create permissions test
     */
    @Test
    public void CreateUserPermissionTest() throws UnauthorizedException, InvalidArgumentException {
        User currentUser = mock(User.class);
        when(currentUser.getUserId()).thenReturn(10l);
        EbeanServer s = mock(EbeanServer.class);
        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                s,
                this.defaultPolicy);
        ArgumentCaptor<UserPermission> argumentCaptor = ArgumentCaptor.forClass(UserPermission.class);
        permissionManager.createUserPermission(currentUser, 0l, 0l, PermissionLevel.WRITE);
        verify(s).save(argumentCaptor.capture());
        UserPermission createdPermission = argumentCaptor.getValue();
        assertEquals(createdPermission.getCanRead(), false);
        assertEquals(createdPermission.getCanWrite(), true);
        assertEquals((long)createdPermission.getFile().getFileId(), 0l);
        assertEquals((long)createdPermission.getUser().getUserId(), 0l);
    }

    @Test(expected = UnauthorizedException.class)
    public void CreateUserPermissionIsAuthorizedTest() throws UnauthorizedException, InvalidArgumentException {
        User currentUser = mock(User.class);
        Policy spec = mock(Policy.class);
        when(spec.CanCreateUserPermission(any(File.class), any(User.class))).thenReturn(false);
        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                spec);
        permissionManager.createUserPermission(currentUser, 0l, 0l, PermissionLevel.WRITE);
    }
}
