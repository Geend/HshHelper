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
    private static Group adminGroup;

    @BeforeClass
    public static void setup() {
        admin = new User("admin", "admin@admin.com", "admin", true, 10);
        adminTwo = new User("adminTwo", "adminTwo@admin.com", "admin", true, 10);
        peter = new User("peter", "peter@gmx.com", "peter", true, 10);
        klaus = new User("klaus", "klaus@gmx.com", "klaus", true, 10);
        adminGroup = new Group("Administrators", admin);
        adminGroup.isAdminGroup = true;
        adminGroup.members = Stream.of(admin, adminTwo).collect(Collectors.toSet());
        admin.groups = Stream.of(adminGroup).collect(Collectors.toSet());
        adminTwo.groups = Stream.of(adminGroup).collect(Collectors.toSet());
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
    public void ownerCannotBeRemovedFromGroup() {
        boolean actual = Specification.CanRemoveGroupMember(peter, adminGroup, admin);
        assertThat(actual).isFalse();
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

    @Test(expected = Exception.class)
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

    @Test(expected = Exception.class)
    public void nonAuthorizedCantDeleteUser() {
        boolean actual = Specification.CanDeleteUser(null, peter);
        assertThat(actual).isFalse();
    }
}
