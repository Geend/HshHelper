import models.*;
import org.junit.Before;
import policyenforcement.Policy;
import policyenforcement.session.Session;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.booleanThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PolicyTests {

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
        allGroup.setMembers(Stream.of(admin, adminTwo, peter, klaus, horst, rudi).collect(Collectors.toList()));

        adminGroup = new Group("Administrators", admin);
        adminGroup.setIsAdminGroup(true);
        adminGroup.setMembers(Stream.of(admin, adminTwo).collect(Collectors.toList()));

        petersGroup = new Group("Peters Group", peter);
        petersGroup.setMembers(Stream.of(peter, klaus, adminTwo).collect(Collectors.toList()));

        klausGroup = new Group("Klaus Group", klaus);
        klausGroup.setMembers(Stream.of(klaus, rudi).collect(Collectors.toList()));

        admin.setGroups(Stream.of(adminGroup, allGroup).collect(Collectors.toList()));
        adminTwo.setGroups(Stream.of(adminGroup, petersGroup, allGroup).collect(Collectors.toList()));
        peter.setGroups(Stream.of(petersGroup, allGroup).collect(Collectors.toList()));
        klaus.setGroups(Stream.of(petersGroup, allGroup).collect(Collectors.toList()));
        horst.setGroups(Stream.of(allGroup).collect(Collectors.toList()));
        rudi.setGroups(Stream.of(allGroup).collect(Collectors.toList()));

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
        boolean actual = Policy.instance.CanCreateUser(admin);
        assertThat(actual).isTrue();
    }

    @Test
    public void nonAdminCantCreateUser() {
        boolean actual = Policy.instance.CanCreateUser(peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void unauthorizedCantCreateUser() {
        boolean actual = Policy.instance.CanCreateUser(null);
        assertThat(actual).isFalse();
    }

    /*
        Delete User
     */
    @Test
    public void adminCanDeleteUser() {
        boolean actual = Policy.instance.CanDeleteUser(admin, peter);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminCantDeleteAdminOwner() {
        boolean actual = Policy.instance.CanDeleteUser(adminTwo, admin);
        assertThat(actual).isFalse();
    }

    @Test
    public void adminCanDeleteOtherAdmin() {
        boolean actual = Policy.instance.CanDeleteUser(admin, adminTwo);
        assertThat(actual).isTrue();
    }

    @Test
    public void nonAdminCantDeleteUser() {
        boolean actual = Policy.instance.CanDeleteUser(peter, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void userCantDeleteHimself() {
        boolean actual = Policy.instance.CanDeleteUser(peter, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedCantDeleteUser() {
        boolean actual = Policy.instance.CanDeleteUser(null, peter);
        assertThat(actual).isFalse();
    }

    /*
        Remove user from Group
     */
    @Test
    public void noUserCanBeRemovedFromAllGroup() {
        boolean actual = Policy.instance.CanRemoveGroupMember(admin, allGroup, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCannotBeRemovedFromGroup() {
        boolean actual = Policy.instance.CanRemoveGroupMember(peter, petersGroup, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCannotBeRemovedFromGroupByAdmin() {
        boolean actual = Policy.instance.CanRemoveGroupMember(admin, petersGroup, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCanRemoveMember() {
        boolean actual = Policy.instance.CanRemoveGroupMember(peter, petersGroup, klaus);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminCanRemoveMemberFromForeignGroup() {
        boolean actual = Policy.instance.CanRemoveGroupMember(admin, petersGroup, klaus);
        assertThat(actual).isTrue();
    }

    @Test
    public void memberCannotRemoveOtherMembersFromGroup() {
        boolean actual = Policy.instance.CanRemoveGroupMember(klaus, petersGroup, adminTwo);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonMemberCannotRemoveOtherMembersFromGroup() {
        boolean actual = Policy.instance.CanRemoveGroupMember(horst, petersGroup, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonMemberCannotRemoveOwnerFromGroup() {
        boolean actual = Policy.instance.CanRemoveGroupMember(horst, petersGroup, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedCannotRemoveGroupMember() {
        boolean actual = Policy.instance.CanRemoveGroupMember(null, petersGroup, peter);
        assertThat(actual).isFalse();
    }

    /*
        Generally adding a user to a Group
     */
    @Test
    public void ownerCanGenerallyAddGroupMember() {
        boolean actual = Policy.instance.CanGenerallyAddGroupMember(peter, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminMemberCanGenerallyAddGroupMember() {
        boolean actual = Policy.instance.CanGenerallyAddGroupMember(adminTwo, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminNonMemberCanGenerallyAddGroupMember() {
        boolean actual = Policy.instance.CanGenerallyAddGroupMember(admin, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void memberCannotGenerallyAddGroupMember() {
        boolean actual = Policy.instance.CanGenerallyAddGroupMember(klaus, petersGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonMemberCannotGenerallyAddGroupMember() {
        boolean actual = Policy.instance.CanGenerallyAddGroupMember(rudi, petersGroup);
        assertThat(actual).isFalse();
    }

    /*
        Add User to Group
     */
    @Test
    public void ownerCanAddGroupMember() {
        boolean actual = Policy.instance.CanAddSpecificGroupMember(peter, petersGroup, admin);
        assertThat(actual).isTrue();
    }

    @Test
    public void foreignAdminCanAddGroupMember() {
        boolean actual = Policy.instance.CanAddSpecificGroupMember(admin, petersGroup, horst);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminGroupMemberCanAddGroupMember() {
        boolean actual = Policy.instance.CanAddSpecificGroupMember(adminTwo, petersGroup, horst);
        assertThat(actual).isTrue();
    }

    @Test
    public void ownerCannotAddMemberTwice() {
        boolean actual = Policy.instance.CanAddSpecificGroupMember(peter, petersGroup, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void foreignAdminCannotAddGroupMemberTwice() {
        boolean actual = Policy.instance.CanAddSpecificGroupMember(admin, petersGroup, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void adminGroupMemberCannotAddGroupMemberTwice() {
        boolean actual = Policy.instance.CanAddSpecificGroupMember(adminTwo, petersGroup, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void groupMemberCannotAddOtherGroupMember() {
        boolean actual = Policy.instance.CanAddSpecificGroupMember(klaus, petersGroup, horst);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonGroupMemberCannotAddOtherGroupMember() {
        boolean actual = Policy.instance.CanAddSpecificGroupMember(horst, petersGroup, rudi);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedUserCannotAddOtherGroupMember() {
        boolean actual = Policy.instance.CanAddSpecificGroupMember(null, petersGroup, rudi);
        assertThat(actual).isFalse();
    }

    /*
        Create Group
     */
    @Test
    public void adminCanCreateGroup() {
        boolean actual = Policy.instance.CanCreateGroup(admin);
        assertThat(actual).isTrue();
    }

    @Test
    public void userCanCreateGroup() {
        boolean actual = Policy.instance.CanCreateGroup(peter);
        assertThat(actual).isTrue();
    }

    @Test
    public void nonAuthorizedUserCannotCreateGroup() {
        boolean actual = Policy.instance.CanCreateGroup(null);
        assertThat(actual).isFalse();
    }


    /*
        See Group Details (members)/group page
     */
    @Test
    public void foreignAdminCanViewGroupDetails() {
        boolean actual = Policy.instance.CanViewGroupDetails(admin, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminGroupMemberCanViewGroupDetails() {
        boolean actual = Policy.instance.CanViewGroupDetails(adminTwo, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void groupOwnerCanViewGroupDetails() {
        boolean actual = Policy.instance.CanViewGroupDetails(peter, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void groupMemberCanViewGroupDetails() {
        boolean actual = Policy.instance.CanViewGroupDetails(klaus, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void nonGroupMemberCannotViewGroupDetails() {
        boolean actual = Policy.instance.CanViewGroupDetails(horst, petersGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void notAuthorizedUserCannotViewGroupDetails() {
        boolean actual = Policy.instance.CanViewGroupDetails(null, petersGroup);
        assertThat(actual).isFalse();
    }

    /*
        View a list of *all* Groups
     */
    @Test
    public void adminCanViewAllGroups() {
        boolean actual = Policy.instance.CanViewAllGroupsList(admin);
        assertThat(actual).isTrue();
    }

    @Test
    public void memberCannotViewAllGroups() {
        boolean actual = Policy.instance.CanViewAllGroupsList(peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void notAuthorizedUserCannotViewAllGroups() {
        boolean actual = Policy.instance.CanViewAllGroupsList(null);
        assertThat(actual).isFalse();
    }

    /*
        Deleting a Group
     */
    @Test
    public void ownerCanDeleteGroup() {
        boolean actual = Policy.instance.CanDeleteGroup(peter, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void foreignAdminCanDeleteGroup() {
        boolean actual = Policy.instance.CanDeleteGroup(admin, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminGroupMemberCanDeleteGroup() {
        boolean actual = Policy.instance.CanDeleteGroup(adminTwo, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void groupMemberCannotDeleteGroup() {
        boolean actual = Policy.instance.CanDeleteGroup(klaus, petersGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonGroupMemberCannotDeleteGroup() {
        boolean actual = Policy.instance.CanDeleteGroup(horst, petersGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedUserCannotDeleteGroup() {
        boolean actual = Policy.instance.CanDeleteGroup(null, petersGroup);
        assertThat(actual).isFalse();
    }

    /*
        Delete Admin Group
     */
    @Test
    public void adminGroupOwnerCannotDeleteAdminGroup() {
        boolean actual = Policy.instance.CanDeleteGroup(admin, adminGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonGroupOwningAdminCannotDeleteAdminGroup() {
        boolean actual = Policy.instance.CanDeleteGroup(adminTwo, adminGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAdminCannotDeleteAdminGroup() {
        boolean actual = Policy.instance.CanDeleteGroup(horst, adminGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void notAuthorizedUserCannotDeleteAdminGroup() {
        boolean actual = Policy.instance.CanDeleteGroup(null, adminGroup);
        assertThat(actual).isFalse();
    }

    /*
        Delete All Group
     */
    @Test
    public void allGroupOwnerCannotDeleteAllGroup() {
        boolean actual = Policy.instance.CanDeleteGroup(admin, allGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonGroupOwningAdminCannotDeleteAllGroup() {
        boolean actual = Policy.instance.CanDeleteGroup(adminTwo, allGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAdminCannotDeleteAllGroup() {
        boolean actual = Policy.instance.CanDeleteGroup(horst, allGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void notAuthorizedUserCannotDeleteAllGroup() {
        boolean actual = Policy.instance.CanDeleteGroup(null, allGroup);
        assertThat(actual).isFalse();
    }

    /*
        Password reset
     */
    @Test
    public void adminCannotResetPassword() {
        boolean actual = Policy.instance.CanResetPassword(admin);
        assertThat(actual).isFalse();
    }

    @Test
    public void userCanResetPassword() {
        boolean actual = Policy.instance.CanResetPassword(peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedUserCanResetPassword() {
        boolean actual = Policy.instance.CanResetPassword(null);
        assertThat(actual).isTrue();
    }

    /*
        Password Update
     */
    @Test
    public void adminCannotUpdateOthersPassword() {
        boolean actual = Policy.instance.CanUpdatePassword(admin, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void userCannotUpdateOthersPassword() {
        boolean actual = Policy.instance.CanUpdatePassword(peter, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void adminCanUpdateOwnPassword() {
        boolean actual = Policy.instance.CanUpdatePassword(admin, admin);
        assertThat(actual).isTrue();
    }

    @Test
    public void userCanUpdateOwnPassword() {
        boolean actual = Policy.instance.CanUpdatePassword(peter, peter);
        assertThat(actual).isTrue();
    }

    @Test
    public void notAuthorizedCannotUpdateOthersPassword() {
        boolean actual = Policy.instance.CanUpdatePassword(null, peter);
        assertThat(actual).isFalse();
    }

    /*
        SessionDeletion Test
     */
    @Test
    public void adminCannotDeleteOthersSession() {
        boolean actual = Policy.instance.CanDeleteSession(admin, petersSession);
        assertThat(actual).isFalse();
    }

    @Test
    public void userCannotDeleteOthersSession() {
        boolean actual = Policy.instance.CanDeleteSession(klaus, petersSession);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedUserCannotDeleteOthersSession() {
        boolean actual = Policy.instance.CanDeleteSession(null, petersSession);
        assertThat(actual).isFalse();
    }

    @Test
    public void userCanDeleteOwnSession() {
        boolean actual = Policy.instance.CanDeleteSession(peter, petersSession);
        assertThat(actual).isTrue();
    }


    /*
        File Read PersmissionTest
     */
    @Test
    public void ownerCanReadFile(){
        boolean actual = Policy.instance.CanReadFile(klaus, klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void userWithUserPermissionCanReadFile(){
        boolean actual = Policy.instance.CanReadFile(peter, klausFile);
        assertThat(actual).isTrue();
    }
    @Test
    public void userWithGroupPermissionCanReadFile(){
        boolean actual = Policy.instance.CanReadFile(klaus, petersGroupFile);
        assertThat(actual).isTrue();

    }
    @Test
    public void userWithGroupPermissionWithoutReadTrueCantReadFile(){
        boolean actual = Policy.instance.CanReadFile(admin, petersGroupFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void normalUserCantReadFile(){
        boolean actual = Policy.instance.CanReadFile(horst, klausFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonOwnerCantViewFilePermissions() {
        boolean actual = Policy.instance.CanViewFilePermissions(horst, klausFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonOwnerAdminCantViewFilePermissions() {
        boolean actual = Policy.instance.CanViewFilePermissions(admin, klausFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCanViewFilePermissions() {
        boolean actual = Policy.instance.CanViewFilePermissions(klaus, klausFile);
        assertThat(actual).isTrue();
    }

    /*
     File Write PersmissionTest
  */
    @Test
    public void ownerCanWriteFile(){
        boolean actual = Policy.instance.CanWriteFile(klaus, klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void userWithUserPermissionCanWriteFile(){
        boolean actual = Policy.instance.CanWriteFile(peter, klausFile);
        assertThat(actual).isTrue();
    }
    @Test
    public void userWithGroupPermissionCanWriteFile(){
        boolean actual = Policy.instance.CanWriteFile(klaus, petersGroupFile);
        assertThat(actual).isTrue();

    }
    @Test
    public void userWithGroupPermissionWithoutWriteTrueCantWriteFile(){
        boolean actual = Policy.instance.CanWriteFile(rudi, petersGroupFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void groupPermissionWithReadTrueWithoutWriteTrueCantWriteFile(){
        boolean actual = Policy.instance.CanWriteFile(rudi, klausFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void normalUserCantWriteFile(){
        boolean actual = Policy.instance.CanWriteFile(horst, klausFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCanDeleteFile() {
        boolean actual = Policy.instance.CanDeleteFile(klaus, klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void normalUserCannotDeleteFile() {
        boolean actual = Policy.instance.CanDeleteFile(horst, klausFile);
        assertThat(actual).isFalse();
    }
    /*
        Zugriff auf Temporäre Datei
     */
    @Test
    public void adminCannotAccessTempFile() {
        boolean actual = Policy.instance.CanAccessTempFile(admin, klausTempFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAdminNonOwnerCannotAccessTempFile() {
        boolean actual = Policy.instance.CanAccessTempFile(peter, klausTempFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCanAccessTempFile() {
        boolean actual = Policy.instance.CanAccessTempFile(klaus, klausTempFile);
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
        assertThat(Policy.instance.CanDeleteGroupPermission(null, permission)).isFalse();
        assertThat(Policy.instance.CanDeleteGroupPermission(user, null)).isFalse();
        assertThat(Policy.instance.CanDeleteGroupPermission(user, permission)).isFalse();
        when(file.getOwner()).thenReturn(user);
        assertThat(Policy.instance.CanDeleteGroupPermission(user, permission)).isTrue();
    }

    @Test
    public void CanDeleteUserPermissionTest() {
        User otherUser = mock(User.class);
        User user = mock(User.class);
        File file = mock(File.class);
        UserPermission permission = mock(UserPermission.class);
        when(file.getOwner()).thenReturn(otherUser);
        when(permission.getFile()).thenReturn(file);
        assertThat(Policy.instance.CanDeleteUserPermission(null, permission)).isFalse();
        assertThat(Policy.instance.CanDeleteUserPermission(user, null)).isFalse();
        assertThat(Policy.instance.CanDeleteUserPermission(user, permission)).isFalse();
        when(file.getOwner()).thenReturn(user);
        assertThat(Policy.instance.CanDeleteUserPermission(user, permission)).isTrue();
    }

    @Test
    public void CanEditUserPermissionTest() {
        User otherUser = mock(User.class);
        User user = mock(User.class);
        File file = mock(File.class);
        UserPermission permission = mock(UserPermission.class);
        when(file.getOwner()).thenReturn(otherUser);
        when(permission.getFile()).thenReturn(file);
        assertThat(Policy.instance.CanEditUserPermission(null, permission)).isFalse();
        assertThat(Policy.instance.CanEditUserPermission(user, null)).isFalse();
        assertThat(Policy.instance.CanEditUserPermission(user, permission)).isFalse();
        when(file.getOwner()).thenReturn(user);
        assertThat(Policy.instance.CanEditUserPermission(user, permission)).isTrue();
    }

    @Test
    public void CanEditGroupPermissionTest() {
        User otherUser = mock(User.class);
        User user = mock(User.class);
        File file = mock(File.class);
        GroupPermission permission = mock(GroupPermission.class);
        when(file.getOwner()).thenReturn(otherUser);
        when(permission.getFile()).thenReturn(file);
        assertThat(Policy.instance.CanEditGroupPermission(null, permission)).isFalse();
        assertThat(Policy.instance.CanEditGroupPermission(user, null)).isFalse();
        assertThat(Policy.instance.CanEditGroupPermission(user, permission)).isFalse();
        when(file.getOwner()).thenReturn(user);
        assertThat(Policy.instance.CanEditGroupPermission(user, permission)).isTrue();
    }
}
