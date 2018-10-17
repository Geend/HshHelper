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
    private static User peter;
    private static Group adminGroup;

    @BeforeClass
    public static void setup() {
        admin = new User("admin", "admin@admin.com", "admin", true, 10);
        peter = new User("peter", "peter@gmx.com", "peter", true, 10);
        adminGroup = new Group("Administrators", admin);
        adminGroup.members = Stream.of(admin, peter).collect(Collectors.toSet());
    }

    @Test
    public void ownerCannotBeRemovedFromGroup() {
        boolean actual = Specification.CanRemoveGroupMemeber(peter, adminGroup, admin);
        assertThat(actual).isFalse();
    }
}
