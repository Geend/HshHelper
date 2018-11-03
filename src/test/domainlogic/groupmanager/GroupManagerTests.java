package domainlogic.groupmanager;

import domainlogic.InvalidArgumentException;
import domainlogic.UnauthorizedException;
import extension.HashHelper;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import models.Group;
import models.User;
import models.finders.GroupFinder;
import models.finders.UserFinder;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;
import java.util.Set;
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
        all.setMembers(Stream.of(admin, peter, klaus).collect(Collectors.toSet()));
        admins.setMembers(Stream.of(admin).collect(Collectors.toSet()));
        petersGroup.setMembers(Stream.of(admin, peter).collect(Collectors.toSet()));

        admin.setGroups(Stream.of(all, admins, petersGroup).collect(Collectors.toSet()));
        peter.setGroups(Stream.of(all, petersGroup).collect(Collectors.toSet()));
        klaus.setGroups(Stream.of(all).collect(Collectors.toSet()));

        gm = new GroupManager(groupFinder, userFinder, defaultServer);
    }

    @Test
    public void canCreateGroup() throws GroupNameAlreadyExistsException, InvalidArgumentException {
        when(defaultServer.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));
        when(userFinder.byIdOptional(adminId)).thenReturn(Optional.of(admin));

        String groupName = "TestGroup";
        gm.createGroup(adminId, groupName);
        verify(defaultServer).save(new Group("TestGroup", admin));
    }

    @Test
    public void cannotCreateGroupWithAnExistingName() throws GroupNameAlreadyExistsException, InvalidArgumentException {
        String groupName = "All";

        when(defaultServer.beginTransaction(any(TxIsolation.class))).thenReturn(mock(Transaction.class));
        when(userFinder.byIdOptional(adminId)).thenReturn(Optional.of(admin));
        when(groupFinder.byName("All")).thenReturn(Optional.of(all));

        expected.expect(GroupNameAlreadyExistsException.class);
        gm.createGroup(adminId, groupName);
        verify(defaultServer, never()).save(all);
    }

    @Test
    public void canGetOwnGroups() throws InvalidArgumentException {
        when(userFinder.byIdOptional(adminId)).thenReturn(Optional.of(admin));

        Set<Group> ownGroups = gm.getOwnGroups(adminId);
        assertThat(ownGroups).containsExactlyInAnyOrder(all, admins, petersGroup);
    }

    @Test
    public void canGetAGroup() {

    }

    @Test
    public void canSeeGroupMembers() throws UnauthorizedException, InvalidArgumentException {
        when(userFinder.byIdOptional(adminId)).thenReturn(Optional.of(admin));
        when(groupFinder.byIdOptional(allId)).thenReturn(Optional.of(all));

        Set<User> users = gm.getGroupMembers(adminId, allId);
        assertThat(users).containsExactlyInAnyOrder(admin, klaus, peter);
    }

    @Test
    public void cannotSeeGroupMembersOfAnotherGroup() throws UnauthorizedException, InvalidArgumentException {
        when(userFinder.byIdOptional(klausId)).thenReturn(Optional.of(klaus));
        when(groupFinder.byIdOptional(adminsGrpId)).thenReturn(Optional.of(admins));

        expected.expect(UnauthorizedException.class);
        Set<User> users = gm.getGroupMembers(klausId, adminsGrpId);
    }

    @Test
    public void canRemoveGroupMember() throws UnauthorizedException, InvalidArgumentException {
        when(userFinder.byIdOptional(adminId)).thenReturn(Optional.of(admin));
        when(groupFinder.byIdOptional(allId)).thenReturn(Optional.of(all));
        when(userFinder.byIdOptional(klausId)).thenReturn(Optional.of(klaus));

        gm.removeGroupMember(adminId, klausId, allId);
        verify(defaultServer).save(all);
        assertThat(all.getMembers()).containsExactlyInAnyOrder(admin, peter);
    }

    @Test
    public void cannotRemoveGroupMemberOfAnotherGroup() throws UnauthorizedException, InvalidArgumentException {
        when(userFinder.byIdOptional(peterId)).thenReturn(Optional.of(peter));
        when(groupFinder.byIdOptional(allId)).thenReturn(Optional.of(all));
        when(userFinder.byIdOptional(klausId)).thenReturn(Optional.of(klaus));

        expected.expect(UnauthorizedException.class);
        gm.removeGroupMember(peterId, klausId, allId);
        verify(defaultServer, never()).save(all);
    }

    @Test
    public void canAddGroupMember() throws UnauthorizedException, InvalidArgumentException {
        when(userFinder.byIdOptional(peterId)).thenReturn(Optional.of(peter));
        when(groupFinder.byIdOptional(petersGrpId)).thenReturn(Optional.of(petersGroup));
        when(userFinder.byIdOptional(klausId)).thenReturn(Optional.of(klaus));

        gm.addGroupMember(peterId, klausId, petersGrpId);
        verify(defaultServer).save(petersGroup);
        assertThat(petersGroup.getMembers()).containsExactlyInAnyOrder(klaus, peter, admin);
    }

    @Test
    public void cannotAddGroupMemberToAnotherGroup() throws UnauthorizedException, InvalidArgumentException {
        when(userFinder.byIdOptional(peterId)).thenReturn(Optional.of(peter));
        when(groupFinder.byIdOptional(allId)).thenReturn(Optional.of(all));
        when(userFinder.byIdOptional(klausId)).thenReturn(Optional.of(klaus));

        expected.expect(UnauthorizedException.class);
        gm.addGroupMember(peterId, klausId, allId);
        verify(defaultServer, never()).save(klaus);
    }

    @Test
    public void canDeleteGroup() throws UnauthorizedException, InvalidArgumentException {
        when(userFinder.byIdOptional(peterId)).thenReturn(Optional.of(peter));
        when(groupFinder.byIdOptional(petersGrpId)).thenReturn(Optional.of(petersGroup));

        gm.deleteGroup(peterId, petersGrpId);
        verify(defaultServer).delete(petersGroup);
    }

    @Test
    public void cannotDeleteGroupAnotherGroup() throws UnauthorizedException, InvalidArgumentException {
        when(userFinder.byIdOptional(peterId)).thenReturn(Optional.of(peter));
        when(groupFinder.byIdOptional(allId)).thenReturn(Optional.of(all));

        expected.expect(UnauthorizedException.class);
        gm.deleteGroup(peterId, allId);
        verify(defaultServer, never()).delete(all);
    }

    @Test
    public void adminCanSeeAllGroups() throws UnauthorizedException, InvalidArgumentException {
        when(userFinder.byIdOptional(adminId)).thenReturn(Optional.of(admin));
        when(groupFinder.byIdOptional(allId)).thenReturn(Optional.of(all));
        when(groupFinder.byIdOptional(petersGrpId)).thenReturn(Optional.of(petersGroup));

        gm.getAllGroups(adminId);
        verify(groupFinder).all();
    }

    @Test
    public void nonAdminCanNotSeeAllGroups() throws UnauthorizedException, InvalidArgumentException {
        when(userFinder.byIdOptional(klausId)).thenReturn(Optional.of(klaus));
        when(groupFinder.byIdOptional(allId)).thenReturn(Optional.of(all));
        when(groupFinder.byIdOptional(petersGrpId)).thenReturn(Optional.of(petersGroup));

        expected.expect(UnauthorizedException.class);
        gm.getAllGroups(klausId);
        verify(groupFinder, never()).all();
    }

    @Test
    public void getAllGroupsNullInput() throws UnauthorizedException, InvalidArgumentException {
        expected.expect(InvalidArgumentException.class);
        gm.getAllGroups(null);
        verify(groupFinder, never()).all();
    }
}
