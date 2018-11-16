import models.*;
import org.junit.Before;
import policy.session.Session;
import org.junit.BeforeClass;
import org.junit.Test;
import policy.Specification;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpecificationTests {

    private static User admin;
    private static User adminTwo;
    private static User peter;
    private static User klaus;
    private static User horst;
    private static User rudi;
    private static Group allGroup;
    private static Group adminGroup;
    private static Group petersGroup;
    private static Group klausGroup;
    private static Session petersSession;

    private static File klausFile;
    private static UserPermission klausFilePeterUserPermission;
    private static GroupPermission klausFileKlausGroupPermission;

    private static File petersGroupFile;
    private static GroupPermission petersGroupFilePetersGroupPermission;
    private static GroupPermission petersGroupFileAdminGroupPermission;

    private static TempFile klausTempFile;


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
        allGroup.setIsAllGroup(true);
        allGroup.setMembers(Stream.of(admin, adminTwo, peter, klaus, horst, rudi).collect(Collectors.toSet()));

        adminGroup = new Group("Administrators", admin);
        adminGroup.setIsAdminGroup(true);
        adminGroup.setMembers(Stream.of(admin, adminTwo).collect(Collectors.toSet()));

        petersGroup = new Group("Peters Group", peter);
        petersGroup.setMembers(Stream.of(peter, klaus, adminTwo).collect(Collectors.toSet()));

        klausGroup = new Group("Klaus Group", klaus);
        klausGroup.setMembers(Stream.of(klaus, rudi).collect(Collectors.toSet()));

        admin.setGroups(Stream.of(adminGroup, allGroup).collect(Collectors.toSet()));
        adminTwo.setGroups(Stream.of(adminGroup, petersGroup, allGroup).collect(Collectors.toSet()));
        peter.setGroups(Stream.of(petersGroup, allGroup).collect(Collectors.toSet()));
        klaus.setGroups(Stream.of(petersGroup, allGroup).collect(Collectors.toSet()));
        horst.setGroups(Stream.of(allGroup).collect(Collectors.toSet()));
        rudi.setGroups(Stream.of(allGroup).collect(Collectors.toSet()));

        byte[] data = {0x42};
        klausFile = new File("klausFile", "Klaus Comment",data,klaus);
        klausFilePeterUserPermission = new UserPermission(klausFile, peter, true, true);

        peter.getUserPermissions().add(klausFilePeterUserPermission);



        petersGroupFile = new File("petersGroupFile", "Peters Group File", data, peter);
        petersGroupFilePetersGroupPermission = new GroupPermission(petersGroupFile,petersGroup, true,true);
        petersGroup.getGroupPermissions().add(petersGroupFilePetersGroupPermission);

        petersGroupFileAdminGroupPermission = new GroupPermission(petersGroupFile, adminGroup, false, false);
        adminGroup.getGroupPermissions().add(petersGroupFileAdminGroupPermission);


        klausFileKlausGroupPermission = new GroupPermission(petersGroupFile, adminGroup, true, false);
        klausGroup.getGroupPermissions().add(klausFileKlausGroupPermission);

        klausTempFile = new TempFile(
            klaus,
            new byte[]{}
        );
    }

    @Before
    public void setupBeforeAll() {
        petersSession = mock(Session.class);
        when(petersSession.getUser()).thenReturn(peter);
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
        boolean actual = Specification.instance.CanCreateUser(admin);
        assertThat(actual).isTrue();
    }

    @Test
    public void nonAdminCantCreateUser() {
        boolean actual = Specification.instance.CanCreateUser(peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void unauthorizedCantCreateUser() {
        boolean actual = Specification.instance.CanCreateUser(null);
        assertThat(actual).isFalse();
    }

    /*
        Delete User
     */
    @Test
    public void adminCanDeleteUser() {
        boolean actual = Specification.instance.CanDeleteUser(admin, peter);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminCantDeleteAdminOwner() {
        boolean actual = Specification.instance.CanDeleteUser(adminTwo, admin);
        assertThat(actual).isFalse();
    }

    @Test
    public void adminCanDeleteOtherAdmin() {
        boolean actual = Specification.instance.CanDeleteUser(admin, adminTwo);
        assertThat(actual).isTrue();
    }

    @Test
    public void nonAdminCantDeleteUser() {
        boolean actual = Specification.instance.CanDeleteUser(peter, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void userCantDeleteHimself() {
        boolean actual = Specification.instance.CanDeleteUser(peter, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedCantDeleteUser() {
        boolean actual = Specification.instance.CanDeleteUser(null, peter);
        assertThat(actual).isFalse();
    }

    /*
        Remove user from Group
     */
    @Test
    public void ownerCannotBeRemovedFromGroup() {
        boolean actual = Specification.instance.CanRemoveGroupMember(peter, petersGroup, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCannotBeRemovedFromGroupByAdmin() {
        boolean actual = Specification.instance.CanRemoveGroupMember(admin, petersGroup, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCanRemoveMember() {
        boolean actual = Specification.instance.CanRemoveGroupMember(peter, petersGroup, klaus);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminCanRemoveMemberFromForeignGroup() {
        boolean actual = Specification.instance.CanRemoveGroupMember(admin, petersGroup, klaus);
        assertThat(actual).isTrue();
    }

    @Test
    public void memberCannotRemoveOtherMembersFromGroup() {
        boolean actual = Specification.instance.CanRemoveGroupMember(klaus, petersGroup, adminTwo);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonMemberCannotRemoveOtherMembersFromGroup() {
        boolean actual = Specification.instance.CanRemoveGroupMember(horst, petersGroup, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonMemberCannotRemoveOwnerFromGroup() {
        boolean actual = Specification.instance.CanRemoveGroupMember(horst, petersGroup, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedCannotRemoveGroupMember() {
        boolean actual = Specification.instance.CanRemoveGroupMember(null, petersGroup, peter);
        assertThat(actual).isFalse();
    }

    /*
        Generally adding a user to a Group
     */
    @Test
    public void ownerCanGenerallyAddGroupMember() {
        boolean actual = Specification.instance.CanGenerallyAddGroupMember(peter, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminMemberCanGenerallyAddGroupMember() {
        boolean actual = Specification.instance.CanGenerallyAddGroupMember(adminTwo, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminNonMemberCanGenerallyAddGroupMember() {
        boolean actual = Specification.instance.CanGenerallyAddGroupMember(admin, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void memberCannotGenerallyAddGroupMember() {
        boolean actual = Specification.instance.CanGenerallyAddGroupMember(klaus, petersGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonMemberCannotGenerallyAddGroupMember() {
        boolean actual = Specification.instance.CanGenerallyAddGroupMember(rudi, petersGroup);
        assertThat(actual).isFalse();
    }

    /*
        Add User to Group
     */
    @Test
    public void ownerCanAddGroupMember() {
        boolean actual = Specification.instance.CanAddSpecificGroupMember(peter, petersGroup, admin);
        assertThat(actual).isTrue();
    }

    @Test
    public void foreignAdminCanAddGroupMember() {
        boolean actual = Specification.instance.CanAddSpecificGroupMember(admin, petersGroup, horst);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminGroupMemberCanAddGroupMember() {
        boolean actual = Specification.instance.CanAddSpecificGroupMember(adminTwo, petersGroup, horst);
        assertThat(actual).isTrue();
    }

    @Test
    public void ownerCannotAddMemberTwice() {
        boolean actual = Specification.instance.CanAddSpecificGroupMember(peter, petersGroup, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void foreignAdminCannotAddGroupMemberTwice() {
        boolean actual = Specification.instance.CanAddSpecificGroupMember(admin, petersGroup, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void adminGroupMemberCannotAddGroupMemberTwice() {
        boolean actual = Specification.instance.CanAddSpecificGroupMember(adminTwo, petersGroup, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void groupMemberCannotAddOtherGroupMember() {
        boolean actual = Specification.instance.CanAddSpecificGroupMember(klaus, petersGroup, horst);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonGroupMemberCannotAddOtherGroupMember() {
        boolean actual = Specification.instance.CanAddSpecificGroupMember(horst, petersGroup, rudi);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedUserCannotAddOtherGroupMember() {
        boolean actual = Specification.instance.CanAddSpecificGroupMember(null, petersGroup, rudi);
        assertThat(actual).isFalse();
    }

    /*
        Create Group
     */
    @Test
    public void adminCanCreateGroup() {
        boolean actual = Specification.instance.CanCreateGroup(admin);
        assertThat(actual).isTrue();
    }

    @Test
    public void userCanCreateGroup() {
        boolean actual = Specification.instance.CanCreateGroup(peter);
        assertThat(actual).isTrue();
    }

    @Test
    public void nonAuthorizedUserCannotCreateGroup() {
        boolean actual = Specification.instance.CanCreateGroup(null);
        assertThat(actual).isFalse();
    }


    /*
        See Group Details (members)/group page
     */
    @Test
    public void foreignAdminCanViewGroupDetails() {
        boolean actual = Specification.instance.CanViewGroupDetails(admin, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminGroupMemberCanViewGroupDetails() {
        boolean actual = Specification.instance.CanViewGroupDetails(adminTwo, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void groupOwnerCanViewGroupDetails() {
        boolean actual = Specification.instance.CanViewGroupDetails(peter, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void groupMemberCanViewGroupDetails() {
        boolean actual = Specification.instance.CanViewGroupDetails(klaus, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void nonGroupMemberCannotViewGroupDetails() {
        boolean actual = Specification.instance.CanViewGroupDetails(horst, petersGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void notAuthorizedUserCannotViewGroupDetails() {
        boolean actual = Specification.instance.CanViewGroupDetails(null, petersGroup);
        assertThat(actual).isFalse();
    }

    /*
        View a list of *all* Groups
     */
    @Test
    public void adminCanViewAllGroups() {
        boolean actual = Specification.instance.CanViewAllGroupsList(admin);
        assertThat(actual).isTrue();
    }

    @Test
    public void memberCannotViewAllGroups() {
        boolean actual = Specification.instance.CanViewAllGroupsList(peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void notAuthorizedUserCannotViewAllGroups() {
        boolean actual = Specification.instance.CanViewAllGroupsList(null);
        assertThat(actual).isFalse();
    }

    /*
        Deleting a Group
     */
    @Test
    public void ownerCanDeleteGroup() {
        boolean actual = Specification.instance.CanDeleteGroup(peter, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void foreignAdminCanDeleteGroup() {
        boolean actual = Specification.instance.CanDeleteGroup(admin, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminGroupMemberCanDeleteGroup() {
        boolean actual = Specification.instance.CanDeleteGroup(adminTwo, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void groupMemberCannotDeleteGroup() {
        boolean actual = Specification.instance.CanDeleteGroup(klaus, petersGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonGroupMemberCannotDeleteGroup() {
        boolean actual = Specification.instance.CanDeleteGroup(horst, petersGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedUserCannotDeleteGroup() {
        boolean actual = Specification.instance.CanDeleteGroup(null, petersGroup);
        assertThat(actual).isFalse();
    }

    /*
        Delete Admin Group
     */
    @Test
    public void adminGroupOwnerCannotDeleteAdminGroup() {
        boolean actual = Specification.instance.CanDeleteGroup(admin, adminGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonGroupOwningAdminCannotDeleteAdminGroup() {
        boolean actual = Specification.instance.CanDeleteGroup(adminTwo, adminGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAdminCannotDeleteAdminGroup() {
        boolean actual = Specification.instance.CanDeleteGroup(horst, adminGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void notAuthorizedUserCannotDeleteAdminGroup() {
        boolean actual = Specification.instance.CanDeleteGroup(null, adminGroup);
        assertThat(actual).isFalse();
    }

    /*
        Delete All Group
     */
    @Test
    public void allGroupOwnerCannotDeleteAllGroup() {
        boolean actual = Specification.instance.CanDeleteGroup(admin, allGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonGroupOwningAdminCannotDeleteAllGroup() {
        boolean actual = Specification.instance.CanDeleteGroup(adminTwo, allGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAdminCannotDeleteAllGroup() {
        boolean actual = Specification.instance.CanDeleteGroup(horst, allGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void notAuthorizedUserCannotDeleteAllGroup() {
        boolean actual = Specification.instance.CanDeleteGroup(null, allGroup);
        assertThat(actual).isFalse();
    }

    /*
        Password reset
     */
    @Test
    public void adminCannotResetPassword() {
        boolean actual = Specification.instance.CanResetPassword(admin);
        assertThat(actual).isFalse();
    }

    @Test
    public void userCanResetPassword() {
        boolean actual = Specification.instance.CanResetPassword(peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedUserCanResetPassword() {
        boolean actual = Specification.instance.CanResetPassword(null);
        assertThat(actual).isTrue();
    }

    /*
        Password Update
     */
    @Test
    public void adminCannotUpdateOthersPassword() {
        boolean actual = Specification.instance.CanUpdatePassword(admin, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void userCannotUpdateOthersPassword() {
        boolean actual = Specification.instance.CanUpdatePassword(peter, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void adminCanUpdateOwnPassword() {
        boolean actual = Specification.instance.CanUpdatePassword(admin, admin);
        assertThat(actual).isTrue();
    }

    @Test
    public void userCanUpdateOwnPassword() {
        boolean actual = Specification.instance.CanUpdatePassword(peter, peter);
        assertThat(actual).isTrue();
    }

    @Test
    public void notAuthorizedCannotUpdateOthersPassword() {
        boolean actual = Specification.instance.CanUpdatePassword(null, peter);
        assertThat(actual).isFalse();
    }

    /*
        SessionDeletion Test
     */
    @Test
    public void adminCannotDeleteOthersSession() {
        boolean actual = Specification.instance.CanDeleteSession(admin, petersSession);
        assertThat(actual).isFalse();
    }

    @Test
    public void userCannotDeleteOthersSession() {
        boolean actual = Specification.instance.CanDeleteSession(klaus, petersSession);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedUserCannotDeleteOthersSession() {
        boolean actual = Specification.instance.CanDeleteSession(null, petersSession);
        assertThat(actual).isFalse();
    }

    @Test
    public void userCanDeleteOwnSession() {
        boolean actual = Specification.instance.CanDeleteSession(peter, petersSession);
        assertThat(actual).isTrue();
    }


    /*
        File Read PersmissionTest
     */
    @Test
    public void ownerCanReadFile(){
        boolean actual = Specification.instance.CanReadFile(klaus, klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void userWithUserPermissionCanReadFile(){
        boolean actual = Specification.instance.CanReadFile(peter, klausFile);
        assertThat(actual).isTrue();
    }
    @Test
    public void userWithGroupPermissionCanReadFile(){
        boolean actual = Specification.instance.CanReadFile(klaus, petersGroupFile);
        assertThat(actual).isTrue();

    }
    @Test
    public void userWithGroupPermissionWithoutReadTrueCantReadFile(){
        boolean actual = Specification.instance.CanReadFile(admin, petersGroupFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void normalUserCantReadFile(){
        boolean actual = Specification.instance.CanReadFile(horst, klausFile);
        assertThat(actual).isFalse();
    }

    /*
     File Write PersmissionTest
  */
    @Test
    public void ownerCanWriteFile(){
        boolean actual = Specification.instance.CanWriteFile(klaus, klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void userWithUserPermissionCanWriteFile(){
        boolean actual = Specification.instance.CanWriteFile(peter, klausFile);
        assertThat(actual).isTrue();
    }
    @Test
    public void userWithGroupPermissionCanWriteFile(){
        boolean actual = Specification.instance.CanWriteFile(klaus, petersGroupFile);
        assertThat(actual).isTrue();

    }
    @Test
    public void userWithGroupPermissionWithoutWriteTrueCantWriteFile(){
        boolean actual = Specification.instance.CanWriteFile(rudi, petersGroupFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void groupPermissionWithReadTrueWithoutWriteTrueCantWriteFile(){
        boolean actual = Specification.instance.CanWriteFile(rudi, klausFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void normalUserCantWriteFile(){
        boolean actual = Specification.instance.CanWriteFile(horst, klausFile);
        assertThat(actual).isFalse();
    }

    /*
        Zugriff auf Temporäre Datei
     */
    @Test
    public void adminCannotAccessTempFile() {
        boolean actual = Specification.instance.CanAccessTempFile(admin, klausTempFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAdminNonOwnerCannotAccessTempFile() {
        boolean actual = Specification.instance.CanAccessTempFile(peter, klausTempFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCanAccessTempFile() {
        boolean actual = Specification.instance.CanAccessTempFile(klaus, klausTempFile);
        assertThat(actual).isTrue();
    }

    /*
        Zugriff auf Berechtigungen
     */

    @Test
    public void CanDeleteGroupPermissionTest() {
        User otherUser = mock(User.class);
        User user = mock(User.class);
        File file = mock(File.class);
        GroupPermission permission = mock(GroupPermission.class);
        when(file.getOwner()).thenReturn(otherUser);
        when(permission.getFile()).thenReturn(file);
        assertThat(Specification.instance.CanDeleteGroupPermission(null, permission)).isFalse();
        assertThat(Specification.instance.CanDeleteGroupPermission(user, null)).isFalse();
        assertThat(Specification.instance.CanDeleteGroupPermission(user, permission)).isFalse();
        when(file.getOwner()).thenReturn(user);
        assertThat(Specification.instance.CanDeleteGroupPermission(user, permission)).isTrue();
    }

    @Test
    public void CanDeleteUserPermissionTest() {
        User otherUser = mock(User.class);
        User user = mock(User.class);
        File file = mock(File.class);
        UserPermission permission = mock(UserPermission.class);
        when(file.getOwner()).thenReturn(otherUser);
        when(permission.getFile()).thenReturn(file);
        assertThat(Specification.instance.CanDeleteUserPermission(null, permission)).isFalse();
        assertThat(Specification.instance.CanDeleteUserPermission(user, null)).isFalse();
        assertThat(Specification.instance.CanDeleteUserPermission(user, permission)).isFalse();
        when(file.getOwner()).thenReturn(user);
        assertThat(Specification.instance.CanDeleteUserPermission(user, permission)).isTrue();
    }

    @Test
    public void CanEditUserPermissionTest() {
        User otherUser = mock(User.class);
        User user = mock(User.class);
        File file = mock(File.class);
        UserPermission permission = mock(UserPermission.class);
        when(file.getOwner()).thenReturn(otherUser);
        when(permission.getFile()).thenReturn(file);
        assertThat(Specification.instance.CanEditUserPermission(null, permission)).isFalse();
        assertThat(Specification.instance.CanEditUserPermission(user, null)).isFalse();
        assertThat(Specification.instance.CanEditUserPermission(user, permission)).isFalse();
        when(file.getOwner()).thenReturn(user);
        assertThat(Specification.instance.CanEditUserPermission(user, permission)).isTrue();
    }

    @Test
    public void CanEditGroupPermissionTest() {
        User otherUser = mock(User.class);
        User user = mock(User.class);
        File file = mock(File.class);
        GroupPermission permission = mock(GroupPermission.class);
        when(file.getOwner()).thenReturn(otherUser);
        when(permission.getFile()).thenReturn(file);
        assertThat(Specification.instance.CanEditGroupPermission(null, permission)).isFalse();
        assertThat(Specification.instance.CanEditGroupPermission(user, null)).isFalse();
        assertThat(Specification.instance.CanEditGroupPermission(user, permission)).isFalse();
        when(file.getOwner()).thenReturn(user);
        assertThat(Specification.instance.CanEditGroupPermission(user, permission)).isTrue();
    }
}
