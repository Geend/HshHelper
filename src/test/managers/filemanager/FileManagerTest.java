package managers.filemanager;

import dtos.file.GroupPermissionDto;
import dtos.file.UserPermissionDto;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.filemanager.dto.FileMeta;
import managers.groupmanager.GroupManager;
import models.*;
import models.finders.FileFinder;
import models.finders.GroupFinder;
import models.finders.UserFinder;
import models.finders.UserQuota;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import policyenforcement.Policy;
import policyenforcement.session.SessionManager;

import javax.swing.text.html.Option;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FileManagerTest {
    private FileFinder fileFinder;
    private UserFinder userFinder;
    private FileManager fileManager;
    private EbeanServer ebeanServer;
    private SessionManager sessionManager;
    private GroupFinder groupFinder;
    private FileMetaFactory fileMetaFactory;
    private Policy policy;
    private Transaction transaction;

    @Before
    public void setup() {
        fileFinder = mock(FileFinder.class);
        userFinder = mock(UserFinder.class);
        ebeanServer = mock(EbeanServer.class);
        sessionManager = mock(SessionManager.class);
        groupFinder = mock(GroupFinder.class);
        fileMetaFactory = mock(FileMetaFactory.class);
        policy = mock(Policy.class);
        transaction = mock(Transaction.class);

        when(ebeanServer.beginTransaction(any(TxIsolation.class))).thenReturn(transaction);
        when(sessionManager.currentPolicy()).thenReturn(policy);

        fileManager = new FileManager(
            fileFinder, userFinder, ebeanServer, sessionManager, groupFinder, fileMetaFactory
        );
    }

    @Test
    public void getGroupPermissionDtosForCreateTest() {
        Group g1 = mock(Group.class);
        Group g2 = mock(Group.class);
        when(g1.getGroupId()).thenReturn(1L);
        when(g1.getName()).thenReturn("Gruppe 1");
        when(g2.getGroupId()).thenReturn(2L);
        when(g2.getName()).thenReturn("Gruppe 2");
        List<Group> userGroups = Arrays.asList(g1, g2);

        User currentUser = mock(User.class);
        when(currentUser.getGroups()).thenReturn(userGroups);

        when(sessionManager.currentUser()).thenReturn(currentUser);

        List<GroupPermissionDto> result = fileManager.getGroupPermissionDtosForCreate();
        assertThat(result.size(), is(2));
        assertThat(result.get(0).getGroupId(), is(1L));
        assertThat(result.get(1).getGroupId(), is(2L));
        assertThat(result.get(0).getGroupName(), is("Gruppe 1"));
        assertThat(result.get(1).getGroupName(), is("Gruppe 2"));
    }

    @Test
    public void getUserPermissionDtosForCreateTest() {
        User currentUser = mock(User.class);
        when(currentUser.getUserId()).thenReturn(3L);

        User user1 = mock(User.class);
        User user2 = mock(User.class);
        when(user1.getUserId()).thenReturn(1L);
        when(user2.getUserId()).thenReturn(2L);
        when(user1.getUsername()).thenReturn("Nutzer 1");
        when(user2.getUsername()).thenReturn("Nutzer 2");
        List<User> allButCurrentUser = Arrays.asList(user1, user2);

        when(sessionManager.currentUser()).thenReturn(currentUser);
        when(userFinder.findAllButThis(3L)).thenReturn(allButCurrentUser);

        List<UserPermissionDto> result = fileManager.getUserPermissionDtosForCreate();
        assertThat(result.size(), is(2));
        assertThat(result.get(0).getUserId(), is(1L));
        assertThat(result.get(1).getUserId(), is(2L));
        assertThat(result.get(0).getUsername(), is("Nutzer 1"));
        assertThat(result.get(1).getUsername(), is("Nutzer 2"));
    }

    @Test
    public void checkQuotaTest() throws QuotaExceededException {
        User currentUser = mock(User.class);
        when(currentUser.getUserId()).thenReturn(1L);
        when(currentUser.getQuotaLimit()).thenReturn(10L);

        UserQuota userQuota = mock(UserQuota.class);
        when(userQuota.getTotalUsage()).thenReturn(8L);
        when(fileFinder.getUsedQuota(1L)).thenReturn(userQuota);

        fileManager.checkQuota(currentUser);
    }

    @Test(expected = QuotaExceededException.class)
    public void checkQuotaTestNotEnoughQuota() throws QuotaExceededException {
        User currentUser = mock(User.class);
        when(currentUser.getUserId()).thenReturn(1L);
        when(currentUser.getQuotaLimit()).thenReturn(10L);

        UserQuota userQuota = mock(UserQuota.class);
        when(userQuota.getTotalUsage()).thenReturn(11L);
        when(fileFinder.getUsedQuota(1L)).thenReturn(userQuota);

        fileManager.checkQuota(currentUser);
    }

    @Test(expected = FilenameAlreadyExistsException.class)
    public void createFileTestFileNameAlreadyExists() throws UnauthorizedException, QuotaExceededException, FilenameAlreadyExistsException {
        String filename = "test.txt";
        Long ownerId = 1L;

        User currentUser = mock(User.class);
        when(currentUser.getUserId()).thenReturn(ownerId);
        when(fileFinder.byFileName(ownerId, filename)).thenReturn(Optional.of(mock(File.class)));
        when(sessionManager.currentUser()).thenReturn(currentUser);

        fileManager.createFile(filename, "xx", new byte[]{}, Collections.emptyList(), Collections.emptyList());
    }

    @Test
    public void createFileTest() throws UnauthorizedException, QuotaExceededException, FilenameAlreadyExistsException {
        String filename = "test.txt";
        String comment = "xxxx";
        byte[] data = new byte[]{1,2,3};
        Long ownerId = 1L;
        DateTime now = DateTime.now();
        DateTimeUtils.setCurrentMillisFixed(now.getMillis());
        Policy policy = mock(Policy.class);

        UserQuota quota = mock(UserQuota.class);
        when(quota.getTotalUsage()).thenReturn(0L);
        User currentUser = mock(User.class);
        when(currentUser.getUserId()).thenReturn(ownerId);
        when(currentUser.getQuotaLimit()).thenReturn(999L);
        when(fileFinder.getUsedQuota(ownerId)).thenReturn(quota);
        when(fileFinder.byFileName(ownerId, filename)).thenReturn(Optional.empty());
        when(sessionManager.currentUser()).thenReturn(currentUser);
        when(sessionManager.currentPolicy()).thenReturn(policy);
        when(policy.canCreateGroupPermission(any(File.class), any(Group.class))).thenReturn(true);
        when(policy.canCreateUserPermission(any(File.class))).thenReturn(true);

        Group g1 = mock(Group.class);
        when(g1.getGroupId()).thenReturn(1L);
        Group g2 = mock(Group.class);
        when(g2.getGroupId()).thenReturn(2L);
        when(groupFinder.byId(1L)).thenReturn(g1);
        when(groupFinder.byId(2L)).thenReturn(g2);

        GroupPermissionDto gp1 = new GroupPermissionDto(1L, null, PermissionLevel.READ);
        GroupPermissionDto gp2 = new GroupPermissionDto(2L, null, PermissionLevel.WRITE);
        List<GroupPermissionDto> groupPermissionDtos = Arrays.asList(gp1, gp2);

        User u2 = mock(User.class);
        when(u2.getUserId()).thenReturn(2L);
        User u3 = mock(User.class);
        when(u3.getUserId()).thenReturn(3L);
        User u4 = mock(User.class);
        when(u4.getUserId()).thenReturn(4L);
        when(userFinder.byId(2L)).thenReturn(u2);
        when(userFinder.byId(3L)).thenReturn(u3);
        when(userFinder.byId(4L)).thenReturn(u4);

        UserPermissionDto upd2 = new UserPermissionDto(2L, null, PermissionLevel.READ);
        UserPermissionDto upd3 = new UserPermissionDto(3L, null, PermissionLevel.WRITE);
        UserPermissionDto upd4 = new UserPermissionDto(4L, null, PermissionLevel.READWRITE);
        List<UserPermissionDto> userPermissionDtos = Arrays.asList(upd2, upd3, upd4);

        fileManager.createFile(filename, comment, data, userPermissionDtos, groupPermissionDtos);

        ArgumentCaptor<Object> savedObjects = ArgumentCaptor.forClass(Object.class);
        verify(ebeanServer, atLeastOnce()).save(savedObjects.capture());

        Optional<File> savedFile = savedObjects.getAllValues().stream().filter(x -> x instanceof File).map(x -> (File)x).findFirst();

        assertThat(savedFile.isPresent(), is(true));
        assertThat(savedFile.get().getOwner(), is(currentUser));
        assertThat(savedFile.get().getComment(), is(comment));
        assertThat(savedFile.get().getName(), is(filename));
        assertThat(savedFile.get().getData(), is(data));
        assertThat(savedFile.get().getWrittenBy(), is(currentUser));
        assertThat(savedFile.get().getWrittenByDt().getMillis(), is(now.getMillis()));

        verify(policy).canCreateGroupPermission(savedFile.get(), g1);
        verify(policy).canCreateGroupPermission(savedFile.get(), g2);

        List<GroupPermission> permissions = savedObjects.getAllValues().stream().filter(x -> x instanceof GroupPermission).map(x -> (GroupPermission)x).collect(Collectors.toList());

        GroupPermission _gp1 = permissions.stream().filter(x -> x.getGroup().getGroupId().equals(1L)).findFirst().get();
        GroupPermission _gp2 = permissions.stream().filter(x -> x.getGroup().getGroupId().equals(2L)).findFirst().get();

        assertThat(_gp1.getFile(), is(savedFile.get()));
        assertThat(_gp1.getGroup(), is(g1));
        assertThat(_gp1.getCanRead(), is(true));
        assertThat(_gp1.getCanWrite(), is(false));
        assertThat(_gp2.getFile(), is(savedFile.get()));
        assertThat(_gp2.getGroup(), is(g2));
        assertThat(_gp2.getCanRead(), is(false));
        assertThat(_gp2.getCanWrite(), is(true));

        verify(policy, times(3)).canCreateUserPermission(savedFile.get());

        List<UserPermission> userPermissions = savedObjects.getAllValues().stream().filter(x -> x instanceof UserPermission).map(x -> (UserPermission)x).collect(Collectors.toList());

        UserPermission up2 = userPermissions.stream().filter(x -> x.getUser().getUserId().equals(2L)).findFirst().get();
        UserPermission up3 = userPermissions.stream().filter(x -> x.getUser().getUserId().equals(3L)).findFirst().get();
        UserPermission up4 = userPermissions.stream().filter(x -> x.getUser().getUserId().equals(4L)).findFirst().get();

        assertThat(up2.getUser(), is(u2));
        assertThat(up2.getCanRead(), is(true));
        assertThat(up2.getCanWrite(), is(false));
        assertThat(up2.getFile(), is(savedFile.get()));

        assertThat(up3.getUser(), is(u3));
        assertThat(up3.getCanRead(), is(false));
        assertThat(up3.getCanWrite(), is(true));
        assertThat(up3.getFile(), is(savedFile.get()));

        assertThat(up4.getUser(), is(u4));
        assertThat(up4.getCanRead(), is(true));
        assertThat(up4.getCanWrite(), is(true));
        assertThat(up4.getFile(), is(savedFile.get()));
    }

    @Test(expected = InvalidArgumentException.class)
    public void getFileMetaTestNonExistentFile() throws UnauthorizedException, InvalidArgumentException {
        Long fileId = 1L;
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.empty());
        fileManager.getFileMeta(fileId);
    }

    @Test(expected = UnauthorizedException.class)
    public void getFileMetaTestNotAuthorized() throws UnauthorizedException, InvalidArgumentException {
        Long fileId = 1L;
        File file = mock(File.class);
        Policy policy = mock(Policy.class);
        when(policy.canGetFileMeta(file)).thenReturn(false);
        when(sessionManager.currentPolicy()).thenReturn(policy);
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.of(file));

        fileManager.getFileMeta(fileId);
    }

    @Test
    public void getFileMetaTest() throws UnauthorizedException, InvalidArgumentException {
        Long fileId = 1L;
        File file = mock(File.class);
        when(policy.canGetFileMeta(file)).thenReturn(true);
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.of(file));
        FileMeta meta = mock(FileMeta.class);
        when(fileMetaFactory.fromFile(file)).thenReturn(meta);
        FileMeta actualMeta = fileManager.getFileMeta(fileId);

        assertThat(actualMeta, is(meta));

        verify(fileMetaFactory).fromFile(file);
    }

    @Test(expected = InvalidArgumentException.class)
    public void getFileContentTestNonExistentFile() throws UnauthorizedException, InvalidArgumentException {
        Long fileId = 1L;
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.empty());
        fileManager.getFileContent(fileId);
    }

    @Test(expected = UnauthorizedException.class)
    public void getFileContentTestNotAuthorized() throws UnauthorizedException, InvalidArgumentException {
        Long fileId = 1L;
        File file = mock(File.class);
        when(policy.canReadFile(file)).thenReturn(false);
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.of(file));

        fileManager.getFileContent(fileId);
    }

    @Test
    public void getFileContentTest() throws UnauthorizedException, InvalidArgumentException {
        Long fileId = 1L;
        byte[] content = new byte[]{1,2,43,4};
        File file = mock(File.class);
        when(file.getData()).thenReturn(content);
        when(policy.canReadFile(file)).thenReturn(true);
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.of(file));

        byte[] actualContent = fileManager.getFileContent(fileId);
        assertThat(actualContent, is(content));
    }

    @Test(expected = InvalidArgumentException.class)
    public void deleteFileTestNonExistentFile() throws UnauthorizedException, InvalidArgumentException {
        Long fileId = 1L;
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.empty());
        fileManager.deleteFile(fileId);
    }

    @Test(expected = UnauthorizedException.class)
    public void deleteFileTestNotAuthorized() throws UnauthorizedException, InvalidArgumentException {
        Long fileId = 1L;
        File file = mock(File.class);
        when(policy.canDeleteFile(file)).thenReturn(false);
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.of(file));

        fileManager.deleteFile(fileId);
    }

    @Test
    public void deleteFileText() throws UnauthorizedException, InvalidArgumentException {
        Long fileId = 1L;
        File file = mock(File.class);
        when(policy.canDeleteFile(file)).thenReturn(true);
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.of(file));

        fileManager.deleteFile(fileId);

        verify(ebeanServer).delete(file);
    }

    @Test(expected = InvalidArgumentException.class)
    public void editFileContentTestNonExistentFile() throws UnauthorizedException, QuotaExceededException, InvalidArgumentException {
        Long fileId = 1L;
        File file = mock(File.class);
        User currentUser = mock(User.class);
        when(currentUser.getUserId()).thenReturn(1L);
        when(sessionManager.currentUser()).thenReturn(currentUser);
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.empty());

        fileManager.editFileContent(fileId, new byte[]{});
    }


    @Test(expected = UnauthorizedException.class)
    public void editFileContentTestNotAuthorized() throws UnauthorizedException, QuotaExceededException, InvalidArgumentException {
        Long fileId = 1L;
        File file = mock(File.class);
        User currentUser = mock(User.class);
        when(currentUser.getUserId()).thenReturn(1L);
        when(sessionManager.currentUser()).thenReturn(currentUser);
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.of(file));
        when(policy.canWriteFile(file)).thenReturn(false);

        fileManager.editFileContent(fileId, new byte[]{});
    }

    @Test
    public void editFileContentTest() throws UnauthorizedException, QuotaExceededException, InvalidArgumentException {
        Long fileId = 1L;
        byte[] newContent = new byte[]{3,2,1,3};
        User currentUser = mock(User.class);
        DateTime now = DateTime.now();
        File file = new File();
        file.setOwner(currentUser);

        DateTimeUtils.setCurrentMillisFixed(now.getMillis());
        UserQuota userQuota = mock(UserQuota.class);
        when(userQuota.getTotalUsage()).thenReturn(0L);
        when(currentUser.getUserId()).thenReturn(1L);
        when(currentUser.getQuotaLimit()).thenReturn(100L);
        when(sessionManager.currentUser()).thenReturn(currentUser);
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.of(file));
        when(fileFinder.getUsedQuota(1L)).thenReturn(userQuota);
        when(policy.canWriteFile(file)).thenReturn(true);

        fileManager.editFileContent(fileId, newContent);

        ArgumentCaptor<File> savedObjects = ArgumentCaptor.forClass(File.class);
        verify(ebeanServer, atLeastOnce()).save(savedObjects.capture());

        assertThat(savedObjects.getValue().getWrittenBy(), is(currentUser));
        assertThat(savedObjects.getValue().getWrittenByDt().getMillis(), is(now.getMillis()));
        assertThat(savedObjects.getValue().getData(), is(newContent));
    }

    @Test
    public void editFileContentTestQuotaExceeded() throws UnauthorizedException, InvalidArgumentException {
        Long fileId = 1L;
        byte[] newContent = new byte[]{3,2,1,3};
        User currentUser = mock(User.class);
        DateTime now = DateTime.now();
        File file = new File();
        file.setOwner(currentUser);

        DateTimeUtils.setCurrentMillisFixed(now.getMillis());
        UserQuota userQuota = mock(UserQuota.class);
        when(userQuota.getTotalUsage()).thenReturn(990L);
        when(currentUser.getUserId()).thenReturn(1L);
        when(currentUser.getQuotaLimit()).thenReturn(100L);
        when(sessionManager.currentUser()).thenReturn(currentUser);
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.of(file));
        when(fileFinder.getUsedQuota(1L)).thenReturn(userQuota);
        when(policy.canWriteFile(file)).thenReturn(true);

        boolean exThrown = false;
        try {
            fileManager.editFileContent(fileId, newContent);
        } catch (QuotaExceededException e) {
            exThrown = true;
        }

        verify(transaction, never()).commit();
        verify(transaction).close();

        assertThat(exThrown, is(true));
    }

    @Test(expected = InvalidArgumentException.class)
    public void editFileCommentTestNonExistentFile() throws QuotaExceededException, UnauthorizedException, InvalidArgumentException {
        Long fileId = 1L;
        User currentUser = mock(User.class);
        when(sessionManager.currentUser()).thenReturn(currentUser);
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.empty());

        fileManager.editFileComment(fileId, "");
    }

    @Test(expected = UnauthorizedException.class)
    public void editFileCommentTestNotAuthorized() throws QuotaExceededException, UnauthorizedException, InvalidArgumentException {
        Long fileId = 1L;
        File file = mock(File.class);
        User currentUser = mock(User.class);
        when(sessionManager.currentUser()).thenReturn(currentUser);
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.of(file));
        when(policy.canWriteFile(file)).thenReturn(false);

        fileManager.editFileComment(fileId, "");
    }

    @Test
    public void editFileCommentTestQuotaExceeded() throws UnauthorizedException, InvalidArgumentException {
        Long fileId = 1L;
        File file = mock(File.class);
        User currentUser = mock(User.class);
        User fileOwner = mock(User.class);
        UserQuota userQuota = mock(UserQuota.class);
        when(file.getOwner()).thenReturn(fileOwner);
        when(userQuota.getTotalUsage()).thenReturn(990L);
        when(fileOwner.getUserId()).thenReturn(2L);
        when(fileOwner.getQuotaLimit()).thenReturn(100L);
        when(sessionManager.currentUser()).thenReturn(currentUser);
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.of(file));
        when(fileFinder.getUsedQuota(2L)).thenReturn(userQuota);
        when(policy.canWriteFile(file)).thenReturn(true);

        boolean exThrown = false;
        try {
            fileManager.editFileComment(fileId, "");
        } catch (QuotaExceededException e) {
            exThrown = true;
        }

        verify(transaction, never()).commit();
        verify(transaction).close();

        assertThat(exThrown, is(true));
    }

    @Test
    public void editFileCommentTest() throws QuotaExceededException, UnauthorizedException, InvalidArgumentException {
        String newComment = "123123";
        Long fileId = 1L;
        File file = new File();
        User currentUser = mock(User.class);
        User fileOwner = mock(User.class);
        UserQuota userQuota = mock(UserQuota.class);
        file.setOwner(fileOwner);
        when(userQuota.getTotalUsage()).thenReturn(10L);
        when(fileOwner.getUserId()).thenReturn(2L);
        when(fileOwner.getQuotaLimit()).thenReturn(100L);
        when(sessionManager.currentUser()).thenReturn(currentUser);
        when(fileFinder.byIdOptional(fileId)).thenReturn(Optional.of(file));
        when(fileFinder.getUsedQuota(2L)).thenReturn(userQuota);
        when(policy.canWriteFile(file)).thenReturn(true);

        fileManager.editFileComment(fileId, newComment);

        ArgumentCaptor<File> savedObjects = ArgumentCaptor.forClass(File.class);
        verify(ebeanServer, atLeastOnce()).save(savedObjects.capture());

        assertThat(savedObjects.getValue().getComment(), is(newComment));
    }


}