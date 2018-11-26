package managers.groupmanager;

import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import extension.HashHelper;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import models.Group;
import models.User;
import models.finders.FileFinder;
import models.finders.GroupFinder;
import models.finders.UserFinder;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import policyenforcement.Policy;
import policyenforcement.session.SessionManager;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GroupManagerTests {

    @Mock
    GroupFinder groupFinder;

    @Mock
    UserFinder userFinder;

    @Mock
    EbeanServer defaultServer;

    @Mock
    SessionManager sessionManager;

    @Mock
    Policy policy;

    @Mock
    FileFinder defaultFileFinder;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException expected = ExpectedException.none();

    public GroupManager gm;

    public static User admin;
    public static Long adminId = 1L;
    public static User peter;
    public static Long peterId = 2L;
    public static User klaus;
    public static Long klausId = 3L;

    public static Group all;
    public static Long allId = 4L;
    public static Group admins;
    public static Long adminsGrpId = 5L;
    public static Group petersGroup;
    public static Long petersGrpId = 6L;

    @BeforeClass
    public static void globalSetup() {
        HashHelper hashHelper = new HashHelper();

        admin = new User("admin", "hsh.helper+admin@gmail.com", hashHelper.hashPassword("admin"), false, 10);
        peter = new User("peter", "hsh.helper+peter@gmail.com",  hashHelper.hashPassword("peter"), false, 10);
        klaus = new User("klaus", "hsh.helper+klaus@gmail.com",  hashHelper.hashPassword("klaus"), false, 10);

        all = new Group("All", admin);
        admins = new Group("Administrators", admin);
        petersGroup = new Group("Peter's Group", peter);

        all.setIsAllGroup(true);
        admins.setIsAdminGroup(true);
    }

    @Before
    public void setup() {
        all.setMembers(Stream.of(admin, peter, klaus).collect(Collectors.toList()));
        admins.setMembers(Stream.of(admin).collect(Collectors.toList()));
        petersGroup.setMembers(Stream.of(admin, peter).collect(Collectors.toList()));

        admin.setGroups(Stream.of(all, admins, petersGroup).collect(Collectors.toList()));
        peter.setGroups(Stream.of(all, petersGroup).collect(Collectors.toList()));
        klaus.setGroups(Stream.of(all).collect(Collectors.toList()));

        gm = new GroupManager(groupFinder, userFinder, defaultServer, sessionManager, policy, defaultFileFinder, null);
    }

    @Test
    public void canCreateGroup() throws GroupNameAlreadyExistsException, InvalidArgumentException {
        when(defaultServer.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));
        when(sessionManager.currentUser()).thenReturn(admin);

        String groupName = "TestGroup";
        gm.createGroup(groupName);

        verify(defaultServer).save(new Group("TestGroup", admin));
    }

    @Test
    public void cannotCreateGroupWithAnExistingName() throws GroupNameAlreadyExistsException, InvalidArgumentException {
        String groupName = "All";

        when(defaultServer.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));
        when(groupFinder.byName("All")).thenReturn(Optional.of(all));
        when(sessionManager.currentUser()).thenReturn(admin);

        expected.expect(GroupNameAlreadyExistsException.class);
        gm.createGroup(groupName);

        verify(defaultServer, never()).save(all);
    }

    @Test
    public void canGetAGroup() {

    }

    @Test
    public void canRemoveGroupMember() throws UnauthorizedException, InvalidArgumentException {
        when(groupFinder.byIdOptional(petersGrpId)).thenReturn(Optional.of(petersGroup));
        when(userFinder.byIdOptional(adminId)).thenReturn(Optional.of(admin));
        when(sessionManager.currentUser()).thenReturn(peter);

        gm.removeGroupMember(adminId, petersGrpId);
        verify(defaultServer).save(petersGroup);
        assertThat(petersGroup.getMembers()).containsExactlyInAnyOrder(peter);
    }

    @Test
    public void cannotRemoveGroupMemberOfAnotherGroup() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentUser()).thenReturn(peter);
        when(groupFinder.byIdOptional(allId)).thenReturn(Optional.of(all));
        when(userFinder.byIdOptional(klausId)).thenReturn(Optional.of(klaus));

        expected.expect(UnauthorizedException.class);
        gm.removeGroupMember(klausId, allId);
        verify(defaultServer, never()).save(all);
    }

    @Test
    public void canAddGroupMember() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentUser()).thenReturn(peter);
        when(groupFinder.byIdOptional(petersGrpId)).thenReturn(Optional.of(petersGroup));
        when(userFinder.byIdOptional(klausId)).thenReturn(Optional.of(klaus));

        gm.addGroupMember(klausId, petersGrpId);
        verify(defaultServer).save(petersGroup);
        assertThat(petersGroup.getMembers()).containsExactlyInAnyOrder(klaus, peter, admin);
    }

    @Test
    public void cannotAddGroupMemberToAnotherGroup() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentUser()).thenReturn(peter);
        when(groupFinder.byIdOptional(allId)).thenReturn(Optional.of(all));
        when(userFinder.byIdOptional(klausId)).thenReturn(Optional.of(klaus));

        expected.expect(UnauthorizedException.class);
        gm.addGroupMember(klausId, allId);
        verify(defaultServer, never()).save(klaus);
    }

    @Test
    public void canDeleteGroup() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentUser()).thenReturn(peter);
        when(groupFinder.byIdOptional(petersGrpId)).thenReturn(Optional.of(petersGroup));

        gm.deleteGroup(petersGrpId);
        verify(defaultServer).delete(petersGroup);
    }

    @Test
    public void cannotDeleteGroupAnotherGroup() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentUser()).thenReturn(peter);
        when(groupFinder.byIdOptional(allId)).thenReturn(Optional.of(all));

        expected.expect(UnauthorizedException.class);
        gm.deleteGroup(allId);
        verify(defaultServer, never()).delete(all);
    }

    @Test
    public void adminCanSeeAllGroups() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentUser()).thenReturn(admin);

        gm.getAllGroups();
        verify(groupFinder).all();
    }

    @Test
    public void nonAdminCanNotSeeAllGroups() throws UnauthorizedException, InvalidArgumentException {
        when(sessionManager.currentUser()).thenReturn(klaus);

        expected.expect(UnauthorizedException.class);
        gm.getAllGroups();
        verify(groupFinder, never()).all();
    }
}
