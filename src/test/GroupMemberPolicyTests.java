import models.Group;
import models.User;
import org.junit.BeforeClass;
import org.junit.Test;
import policy.Specification;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class GroupMemberPolicyTests {

    private static User admin;
    private static User adminTwo;
    private static User peter;
    private static User klaus;
    private static User horst;
    private static User rudi;
    private static Group allGroup;
    private static Group adminGroup;
    private static Group petersGroup;

    /*
        adminGroup:
            admin, adminTwo

        petersGroup:
            peter, klaus, adminTwo

        no *explicit* groups:
            horst, rudi
     */
    @BeforeClass
    public static void setup() {
        admin = new User("admin", "admin@admin.com", "admin", true, 10);
        adminTwo = new User("adminTwo", "adminTwo@admin.com", "admin", true, 10);
        peter = new User("peter", "peter@gmx.com", "peter", true, 10);
        klaus = new User("klaus", "klaus@gmx.com", "klaus", true, 10);
        horst = new User("horst", "horst@gmx.com", "horst", true, 10);
        rudi = new User("rudi", "rudi@gmx.com", "rudi", true, 10);

        allGroup = new Group("Alle", admin);
        allGroup.isAllGroup = true;
        allGroup.members = Stream.of(admin, adminTwo, peter, klaus, horst, rudi).collect(Collectors.toSet());

        adminGroup = new Group("Administrators", admin);
        adminGroup.isAdminGroup = true;
        adminGroup.members = Stream.of(admin, adminTwo).collect(Collectors.toSet());

        petersGroup = new Group("Peters Group", peter);
        petersGroup.members = Stream.of(peter, klaus, adminTwo).collect(Collectors.toSet());

        admin.groups = Stream.of(adminGroup, allGroup).collect(Collectors.toSet());
        adminTwo.groups = Stream.of(adminGroup, petersGroup, allGroup).collect(Collectors.toSet());
        peter.groups = Stream.of(petersGroup, allGroup).collect(Collectors.toSet());
        klaus.groups = Stream.of(petersGroup, allGroup).collect(Collectors.toSet());
        horst.groups = Stream.of(allGroup).collect(Collectors.toSet());
        rudi.groups = Stream.of(allGroup).collect(Collectors.toSet());
    }

    @Test
    public void assertAdminIsAdmin() {
        assertThat(admin.isAdmin()).isTrue();
    }

    @Test
    public void assertAdminTwoIsAdmin() {
        assertThat(admin.isAdmin()).isTrue();
    }

    @Test
    public void assertPeterIsntAdmin() {
        assertThat(peter.isAdmin()).isFalse();
    }

    @Test
    public void assertKlausIsntAdmin() {
        assertThat(klaus.isAdmin()).isFalse();
    }

    @Test
    public void assertHorstIsntAdmin() {
        assertThat(horst.isAdmin()).isFalse();
    }


    /*
        Create User
     */
    @Test
    public void adminCanCreateUser() {
        boolean actual = Specification.CanCreateUser(admin);
        assertThat(actual).isTrue();
    }

    @Test
    public void nonAdminCantCreateUser() {
        boolean actual = Specification.CanCreateUser(peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void unauthorizedCantCreateUser() {
        boolean actual = Specification.CanCreateUser(null);
        assertThat(actual).isFalse();
    }

    /*
        Delete User
     */
    @Test
    public void adminCanDeleteUser() {
        boolean actual = Specification.CanDeleteUser(admin, peter);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminCantDeleteAdminOwner() {
        boolean actual = Specification.CanDeleteUser(adminTwo, admin);
        assertThat(actual).isFalse();
    }

    @Test
    public void adminCanDeleteOtherAdmin() {
        boolean actual = Specification.CanDeleteUser(admin, adminTwo);
        assertThat(actual).isTrue();
    }

    @Test
    public void nonAdminCantDeleteUser() {
        boolean actual = Specification.CanDeleteUser(peter, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void userCantDeleteHimself() {
        boolean actual = Specification.CanDeleteUser(peter, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedCantDeleteUser() {
        boolean actual = Specification.CanDeleteUser(null, peter);
        assertThat(actual).isFalse();
    }

    /*
        Remove user from Group
     */
    @Test
    public void ownerCannotBeRemovedFromGroup() {
        boolean actual = Specification.CanRemoveGroupMember(peter, petersGroup, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCannotBeRemovedFromGroupByAdmin() {
        boolean actual = Specification.CanRemoveGroupMember(admin, petersGroup, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCanRemoveMember() {
        boolean actual = Specification.CanRemoveGroupMember(peter, petersGroup, klaus);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminCanRemoveMemberFromForeignGroup() {
        boolean actual = Specification.CanRemoveGroupMember(admin, petersGroup, klaus);
        assertThat(actual).isTrue();
    }

    @Test
    public void memberCanRemoveHimselfFromGroup() {
        assertThat(true).isFalse();
    }

    @Test
    public void memberCannotRemoveOtherMembersFromGroup() {
        boolean actual = Specification.CanRemoveGroupMember(klaus, petersGroup, adminTwo);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonMemberCannotRemoveOtherMembersFromGroup() {
        boolean actual = Specification.CanRemoveGroupMember(horst, petersGroup, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonMemberCannotRemoveOwnerFromGroup() {
        boolean actual = Specification.CanRemoveGroupMember(horst, petersGroup, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedCannotRemoveGroupMember() {
        boolean actual = Specification.CanRemoveGroupMember(null, petersGroup, peter);
        assertThat(actual).isFalse();
    }

    /*
        Add User to Group
     */
    @Test
    public void ownerCanAddGroupMember() {
        boolean actual = Specification.CanAddGroupMember(peter, petersGroup, admin);
        assertThat(actual).isTrue();
    }

    @Test
    public void foreignAdminCanAddGroupMember() {
        boolean actual = Specification.CanAddGroupMember(admin, petersGroup, horst);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminGroupMemberCanAddGroupMember() {
        boolean actual = Specification.CanAddGroupMember(adminTwo, petersGroup, horst);
        assertThat(actual).isTrue();
    }

    @Test
    public void ownerCannotAddMemberTwice() {
        boolean actual = Specification.CanAddGroupMember(peter, petersGroup, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void foreignAdminCannotAddGroupMemberTwice() {
        boolean actual = Specification.CanAddGroupMember(admin, petersGroup, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void adminGroupMemberCannotAddGroupMemberTwice() {
        boolean actual = Specification.CanAddGroupMember(adminTwo, petersGroup, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void groupMemberCannotAddOtherGroupMember() {
        boolean actual = Specification.CanAddGroupMember(klaus, petersGroup, horst);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonGroupMemberCannotAddOtherGroupMember() {
        boolean actual = Specification.CanAddGroupMember(horst, petersGroup, rudi);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedUserCannotAddOtherGroupMember() {
        boolean actual = Specification.CanAddGroupMember(null, petersGroup, rudi);
        assertThat(actual).isFalse();
    }

    /*
        Create Group
     */
    @Test
    public void adminCanCreateGroup() {
        boolean actual = Specification.CanCreateGroup(admin);
        assertThat(actual).isTrue();
    }

    @Test
    public void userCanCreateGroup() {
        boolean actual = Specification.CanCreateGroup(peter);
        assertThat(actual).isTrue();
    }

    @Test
    public void nonAuthorizedUserCannotCreateGroup() {
        boolean actual = Specification.CanCreateGroup(null);
        assertThat(actual).isFalse();
    }


    /*
        See Group Details (members)/group page
     */
    @Test
    public void foreignAdminCanViewGroupDetails() {
        boolean actual = Specification.CanViewGroupDetails(admin, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminGroupMemberCanViewGroupDetails() {
        boolean actual = Specification.CanViewGroupDetails(adminTwo, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void groupOwnerCanViewGroupDetails() {
        boolean actual = Specification.CanViewGroupDetails(peter, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void groupMemberCanViewGroupDetails() {
        boolean actual = Specification.CanViewGroupDetails(klaus, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void nonGroupMemberCannotViewGroupDetails() {
        boolean actual = Specification.CanViewGroupDetails(horst, petersGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void notAuthorizedUserCannotViewGroupDetails() {
        boolean actual = Specification.CanViewGroupDetails(null, petersGroup);
        assertThat(actual).isFalse();
    }

    /*
        View a list of *all* Groups
     */
    @Test
    public void adminCanViewAllGroups() {
        boolean actual = Specification.CanViewAllGroupsList(admin);
        assertThat(actual).isTrue();
    }

    @Test
    public void memberCannotViewAllGroups() {
        boolean actual = Specification.CanViewAllGroupsList(peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void notAuthorizedUserCannotViewAllGroups() {
        boolean actual = Specification.CanViewAllGroupsList(null);
        assertThat(actual).isFalse();
    }

    /*
        Deleting a Group
     */
    @Test
    public void ownerCanDeleteGroup() {
        boolean actual = Specification.CanDeleteGroup(peter, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void foreignAdminCanDeleteGroup() {
        boolean actual = Specification.CanDeleteGroup(admin, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminGroupMemberCanDeleteGroup() {
        boolean actual = Specification.CanDeleteGroup(adminTwo, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void groupMemberCannotDeleteGroup() {
        boolean actual = Specification.CanDeleteGroup(klaus, petersGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonGroupMemberCannotDeleteGroup() {
        boolean actual = Specification.CanDeleteGroup(horst, petersGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedUserCannotDeleteGroup() {
        boolean actual = Specification.CanDeleteGroup(null, petersGroup);
        assertThat(actual).isFalse();
    }

    /*
        Delete Admin Group
     */
    @Test
    public void adminGroupOwnerCannotDeleteAdminGroup() {
        boolean actual = Specification.CanDeleteGroup(admin, adminGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonGroupOwningAdminCannotDeleteAdminGroup() {
        boolean actual = Specification.CanDeleteGroup(adminTwo, adminGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAdminCannotDeleteAdminGroup() {
        boolean actual = Specification.CanDeleteGroup(horst, adminGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void notAuthorizedUserCannotDeleteAdminGroup() {
        boolean actual = Specification.CanDeleteGroup(null, adminGroup);
        assertThat(actual).isFalse();
    }

    /*
        Delete All Group
     */
    @Test
    public void allGroupOwnerCannotDeleteAllGroup() {
        boolean actual = Specification.CanDeleteGroup(admin, allGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonGroupOwningAdminCannotDeleteAllGroup() {
        boolean actual = Specification.CanDeleteGroup(adminTwo, allGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAdminCannotDeleteAllGroup() {
        boolean actual = Specification.CanDeleteGroup(horst, allGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void notAuthorizedUserCannotDeleteAllGroup() {
        boolean actual = Specification.CanDeleteGroup(null, allGroup);
        assertThat(actual).isFalse();
    }
}
