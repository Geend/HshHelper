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
    private static Group adminGroup;
    private static Group petersGroup;

    @BeforeClass
    public static void setup() {
        admin = new User("admin", "admin@admin.com", "admin", true, 10);
        adminTwo = new User("adminTwo", "adminTwo@admin.com", "admin", true, 10);
        peter = new User("peter", "peter@gmx.com", "peter", true, 10);
        klaus = new User("klaus", "klaus@gmx.com", "klaus", true, 10);
        horst = new User("horst", "horst@gmx.com", "horst", true, 10);

        adminGroup = new Group("Administrators", admin);
        adminGroup.isAdminGroup = true;
        adminGroup.members = Stream.of(admin, adminTwo).collect(Collectors.toSet());

        petersGroup = new Group("Peters Group", peter);
        petersGroup.members = Stream.of(peter, klaus, adminTwo).collect(Collectors.toSet());

        admin.groups = Stream.of(adminGroup).collect(Collectors.toSet());
        adminTwo.groups = Stream.of(adminGroup, petersGroup).collect(Collectors.toSet());
        peter.groups = Stream.of(petersGroup).collect(Collectors.toSet());
        klaus.groups = Stream.of(petersGroup).collect(Collectors.toSet());
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
}
