package managers.permissionmanager;

import dtos.EditGroupPermissionDto;
import dtos.EditUserPermissionDto;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import io.ebean.EbeanServer;
import managers.filemanager.FileManager;
import models.*;
import models.finders.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import policyenforcement.Policy;
import policyenforcement.session.SessionManager;

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
    private SessionManager defaultSessionManager;
    private FileManager defaultFileManager;

    @Before
    public void init() {
        this.defaultUserFinder = mock(UserFinder.class);
        this.defaultPolicy = mock(Policy.class);
        this.defaultGroupFinder = mock(GroupFinder.class);
        this.defaultFileFinder = mock(FileFinder.class);
        this.defaultEbeanServer = mock(EbeanServer.class);
        this.defaultGroupPermissionFinder = mock(GroupPermissionFinder.class);
        this.defaultUserPermissionFinder = mock(UserPermissionFinder.class);
        this.defaultSessionManager = mock(SessionManager.class);
        this.defaultFileManager = mock(FileManager.class);

        when(defaultSessionManager.currentUser()).thenReturn(mock(User.class));
        when(defaultSessionManager.currentPolicy()).thenReturn(defaultPolicy);
        when(defaultPolicy.canDeleteGroupPermission(any(GroupPermission.class))).thenReturn(true);
        when(defaultPolicy.canDeleteUserPermission(any(UserPermission.class))).thenReturn(true);
        when(defaultPolicy.canEditUserPermission(any(UserPermission.class))).thenReturn(true);
        when(defaultPolicy.canEditGroupPermission(any(GroupPermission.class))).thenReturn(true);
        when(defaultPolicy.canCreateUserPermission(any(File.class))).thenReturn(true);
        when(defaultPolicy.canCreateGroupPermission(any(File.class), any(Group.class))).thenReturn(true);

        when(defaultUserFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(User.class)));
        when(defaultGroupFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(Group.class)));
        when(defaultGroupPermissionFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(GroupPermission.class)));
        when(defaultUserPermissionFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(UserPermission.class)));
        when(defaultFileFinder.byIdOptional(0l)).thenReturn(Optional.of(mock(File.class)));

        // Mocks to satisfy logger object accesses during tests
        when(this.defaultSessionManager.currentUser().getUsername()).thenReturn("");
        when(this.defaultGroupPermissionFinder.byIdOptional(0l).get().getGroup()).thenReturn(mock(Group.class));
        when(this.defaultGroupPermissionFinder.byIdOptional(0l).get().getGroup().getName()).thenReturn("");
        when(this.defaultUserPermissionFinder.byIdOptional(0l).get().getUser()).thenReturn(mock(User.class));
        when(this.defaultUserPermissionFinder.byIdOptional(0l).get().getUser().getUsername()).thenReturn("");
    }

    @Test(expected = UnauthorizedException.class)
    public void ShowEditUserPermissionIsAuthorizesTest() throws InvalidDataException, UnauthorizedException, InvalidArgumentException {
        Policy spec = mock(Policy.class);
        when(spec.canEditUserPermission(any(UserPermission.class))).thenReturn(false);
        when(defaultSessionManager.currentPolicy()).thenReturn(spec);

        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                this.defaultSessionManager,
                this.defaultFileManager);
        permissionManager.getUserPermissionForEdit(0l);
    }

    @Test
    public void EditUserPermissionTest() throws UnauthorizedException, InvalidArgumentException {
        UserPermission up = mock(UserPermission.class);
        UserPermissionFinder upf = mock(UserPermissionFinder.class);
        when(upf.byIdOptional(0l)).thenReturn(Optional.of(up));

        // satisfy the logger
        when(up.getUser()).thenReturn(mock(User.class));
        when(up.getUser().getUsername()).thenReturn("");

        EbeanServer s = mock(EbeanServer.class);
        PermissionManager permissionManager = new PermissionManager(
                upf,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                s,
                this.defaultSessionManager,
                this.defaultFileManager);
        permissionManager.editUserPermission(0l, PermissionLevel.WRITE);
        verify(up).setCanRead(false);
        verify(up).setCanWrite(true);
        verify(s).save(up);
    }

    @Test
    public void EditGroupPermissionTest() throws UnauthorizedException, InvalidArgumentException {
        GroupPermission gp = mock(GroupPermission.class);
        GroupPermissionFinder gpf = mock(GroupPermissionFinder.class);
        when(gpf.byIdOptional(0l)).thenReturn(Optional.of(gp));

        // satisfy the logger
        when(gp.getGroup()).thenReturn(mock(Group.class));
        when(gp.getGroup().getName()).thenReturn("");

        EbeanServer s = mock(EbeanServer.class);
        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                gpf,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                s,
                this.defaultSessionManager,
                this.defaultFileManager);
        permissionManager.editGroupPermission(0l, PermissionLevel.READ);
        verify(gp).setCanRead(true);
        verify(gp).setCanWrite(false);
        verify(s).save(gp);
    }

    @Test
    public void ShowEditUserPermissionDataTest() throws InvalidDataException, UnauthorizedException, InvalidArgumentException {
        File file = mock(File.class);
        when(file.getFileId()).thenReturn(123321L);
        when(file.getName()).thenReturn("xx.txt");
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("abc");

        UserPermissionFinder userFinder = mock(UserPermissionFinder.class);
        UserPermission p = mock(UserPermission.class);
        when(p.getCanRead()).thenReturn(true);
        when(p.getCanWrite()).thenReturn(false);
        when(p.getUserPermissionId()).thenReturn(55l);
        when(p.getFile()).thenReturn(file);
        when(p.getUser()).thenReturn(user);
        when(userFinder.byIdOptional(0l)).thenReturn(Optional.of(p));
        PermissionManager permissionManager = new PermissionManager(
                userFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                this.defaultSessionManager,
                this.defaultFileManager);
        EditUserPermissionDto result = permissionManager.getUserPermissionForEdit(0l);
        assertEquals((long)result.getUserPermissionId(), 55l);
        assertEquals(result.getPermissionLevel(), PermissionLevel.READ);
        assertEquals((long)result.getFileId(), 123321L);
        assertEquals(result.getFilename(), "xx.txt");
        assertEquals(result.getUsername(), "abc");
    }

    @Test
    public void ShowEditGroupPermissionDataTest() throws InvalidDataException, UnauthorizedException, InvalidArgumentException {
        File file = mock(File.class);
        when(file.getFileId()).thenReturn(123321L);
        when(file.getName()).thenReturn("xx.txt");
        Group group = mock(Group.class);
        when(group.getGroupId()).thenReturn(3333L);
        when(group.getName()).thenReturn("abc");

        GroupPermissionFinder groupFinder = mock(GroupPermissionFinder.class);
        GroupPermission p = mock(GroupPermission.class);
        when(p.getCanRead()).thenReturn(true);
        when(p.getCanWrite()).thenReturn(false);
        when(p.getGroupPermissionId()).thenReturn(55l);
        when(p.getFile()).thenReturn(file);
        when(p.getGroup()).thenReturn(group);
        when(groupFinder.byIdOptional(0l)).thenReturn(Optional.of(p));
        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                groupFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                this.defaultSessionManager,
                this.defaultFileManager);
        EditGroupPermissionDto result = permissionManager.getGroupPermissionForEdit(0l);
        assertEquals((long)result.getGroupPermissionId(), 55l);
        assertEquals(result.getPermissionLevel(), PermissionLevel.READ);
        assertEquals((long)result.getFileId(), 123321L);
        assertEquals(result.getFilename(), "xx.txt");
        assertEquals((long)result.getGroupId(), 3333L);
        assertEquals(result.getGroupName(), "abc");
    }


    @Test(expected = UnauthorizedException.class)
    public void ShowEditGroupPermissionIsAuthorizesTest() throws InvalidDataException, UnauthorizedException, InvalidArgumentException {
        Policy spec = mock(Policy.class);
        when(spec.canEditGroupPermission(any(GroupPermission.class))).thenReturn(false);
        when(defaultSessionManager.currentPolicy()).thenReturn(spec);

        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                this.defaultSessionManager,
                this.defaultFileManager);
        permissionManager.getGroupPermissionForEdit(0l);
    }

    @Test(expected = UnauthorizedException.class)
    public void DeleteGroupPermissionIsAuthorizedTest() throws UnauthorizedException, InvalidArgumentException {
        Policy spec = mock(Policy.class);
        when(spec.canDeleteGroupPermission(any(GroupPermission.class))).thenReturn(false);
        when(defaultSessionManager.currentPolicy()).thenReturn(spec);

        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                this.defaultSessionManager,
                this.defaultFileManager);
        permissionManager.deleteGroupPermission(0l);
    }

    @Test()
    public void DeleteGroupPermissionTest() throws UnauthorizedException, InvalidArgumentException {
        GroupPermission permission = mock(GroupPermission.class);
        GroupPermissionFinder groupPermissionFinder = mock(GroupPermissionFinder.class);
        when(groupPermissionFinder.byIdOptional(0l)).thenReturn(Optional.of(permission));

        // satisfy the logger
        when(permission.getGroup()).thenReturn(mock(Group.class));
        when(permission.getGroup().getName()).thenReturn("");

        EbeanServer s = mock(EbeanServer.class);
        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                groupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                s,
                this.defaultSessionManager,
                this.defaultFileManager);
        permissionManager.deleteGroupPermission(0l);

        verify(s).delete(permission);
    }

    @Test()
    public void DeleteUserPermissionTest() throws UnauthorizedException, InvalidArgumentException {
        UserPermission permission = mock(UserPermission.class);
        UserPermissionFinder userPermissionFinder = mock(UserPermissionFinder.class);
        when(userPermissionFinder.byIdOptional(0l)).thenReturn(Optional.of(permission));

        // satisfy the logger
        when(permission.getUser()).thenReturn(mock(User.class));
        when(permission.getUser().getUsername()).thenReturn("");

        EbeanServer s = mock(EbeanServer.class);
        PermissionManager permissionManager = new PermissionManager(
                userPermissionFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                s,
                this.defaultSessionManager,
                this.defaultFileManager);
        permissionManager.deleteUserPermission(0l);

        verify(s).delete(permission);
    }

    @Test(expected = UnauthorizedException.class)
    public void DeleteUserPermissionIsAuthorizedTest() throws UnauthorizedException, InvalidArgumentException {
        Policy spec = mock(Policy.class);
        when(spec.canDeleteUserPermission(any(UserPermission.class))).thenReturn(false);
        when(defaultSessionManager.currentPolicy()).thenReturn(spec);

        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                this.defaultSessionManager,
                this.defaultFileManager);
        permissionManager.deleteUserPermission(0l);
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
                this.defaultSessionManager,
                this.defaultFileManager);
        ArgumentCaptor<UserPermission> argumentCaptor = ArgumentCaptor.forClass(UserPermission.class);
        permissionManager.createUserPermission(0l, 0l, PermissionLevel.WRITE);
        verify(s).save(argumentCaptor.capture());
        UserPermission createdPermission = argumentCaptor.getValue();
        assertEquals(createdPermission.getCanRead(), false);
        assertEquals(createdPermission.getCanWrite(), true);
        assertEquals((long)createdPermission.getFile().getFileId(), 0l);
        assertEquals((long)createdPermission.getUser().getUserId(), 0l);
    }

    @Test(expected = UnauthorizedException.class)
    public void CreateUserPermissionIsAuthorizedTest() throws UnauthorizedException, InvalidArgumentException {
        Policy spec = mock(Policy.class);
        when(spec.canCreateUserPermission(any(File.class))).thenReturn(false);
        when(defaultSessionManager.currentPolicy()).thenReturn(spec);

        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                this.defaultSessionManager,
                this.defaultFileManager);
        permissionManager.createUserPermission(0l, 0l, PermissionLevel.WRITE);
    }

    @Test
    public void CreateGroupPermissionTest() throws UnauthorizedException, InvalidArgumentException {
        EbeanServer s = mock(EbeanServer.class);
        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                s,
                this.defaultSessionManager,
                this.defaultFileManager);
        ArgumentCaptor<GroupPermission> argumentCaptor = ArgumentCaptor.forClass(GroupPermission.class);
        permissionManager.createGroupPermission(0l, 0l, PermissionLevel.WRITE);
        verify(s).save(argumentCaptor.capture());
        GroupPermission createdPermission = argumentCaptor.getValue();
        assertEquals(createdPermission.getCanRead(), false);
        assertEquals(createdPermission.getCanWrite(), true);
        assertEquals((long)createdPermission.getFile().getFileId(), 0l);
        assertEquals((long)createdPermission.getGroup().getGroupId(), 0l);
    }

    @Test(expected = UnauthorizedException.class)
    public void CreateGroupPermissionIsAuthorizedTest() throws UnauthorizedException, InvalidArgumentException {
        Policy spec = mock(Policy.class);
        when(spec.canCreateGroupPermission(any(File.class), any(Group.class))).thenReturn(false);
        when(defaultSessionManager.currentPolicy()).thenReturn(spec);

        PermissionManager permissionManager = new PermissionManager(
                this.defaultUserPermissionFinder,
                this.defaultGroupPermissionFinder,
                this.defaultFileFinder,
                this.defaultGroupFinder,
                this.defaultUserFinder,
                this.defaultEbeanServer,
                this.defaultSessionManager,
                this.defaultFileManager);
        permissionManager.createGroupPermission(0l, 0l, PermissionLevel.WRITE);
    }
}
