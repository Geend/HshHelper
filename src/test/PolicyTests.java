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
        admin = new User("admin", "admin@admin.com", "admin", true, 10l);
        adminTwo = new User("adminTwo", "adminTwo@admin.com", "admin", true, 10l);
        peter = new User("peter", "peter@gmx.com", "peter", true, 10l);
        klaus = new User("klaus", "klaus@gmx.com", "klaus", true, 10l);
        horst = new User("horst", "horst@gmx.com", "horst", true, 10l);
        rudi = new User("rudi", "rudi@gmx.com", "rudi", true, 10l);

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
        Policy Initialization
     */
    @Test(expected = IllegalArgumentException.class)
    public void noNulledUserPolicies() {
        Policy.ForUser(null);
    }

    /*
        Create User
     */
    @Test
    public void adminCanCreateUser() {
        boolean actual = Policy.ForUser(admin).canCreateUser();
        assertThat(actual).isTrue();
    }

    @Test
    public void nonAdminCantCreateUser() {
        boolean actual = Policy.ForUser(peter).canCreateUser();
        assertThat(actual).isFalse();
    }

    /*
        Delete User
     */
    @Test
    public void adminCanDeleteUser() {
        boolean actual = Policy.ForUser(admin).canDeleteUser(peter);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminCantDeleteAdminOwner() {
        boolean actual = Policy.ForUser(adminTwo).canDeleteUser(admin);
        assertThat(actual).isFalse();
    }

    @Test
    public void adminCanDeleteOtherAdmin() {
        boolean actual = Policy.ForUser(admin).canDeleteUser(adminTwo);
        assertThat(actual).isTrue();
    }

    @Test
    public void nonAdminCantDeleteUser() {
        boolean actual = Policy.ForUser(peter).canDeleteUser(klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void userCantDeleteHimself() {
        boolean actual = Policy.ForUser(peter).canDeleteUser(peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void adminCantDeleteHimself() {
        boolean actual = Policy.ForUser(adminTwo).canDeleteUser(adminTwo);
        assertThat(actual).isFalse();
    }

    /*
        Remove user from Group
     */
    @Test
    public void noUserCanBeRemovedFromAllGroup() {
        boolean actual = Policy.ForUser(admin).canRemoveGroupMember(allGroup, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCannotBeRemovedFromGroup() {
        boolean actual = Policy.ForUser(peter).canRemoveGroupMember(petersGroup, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCannotBeRemovedFromGroupByAdmin() {
        boolean actual = Policy.ForUser(admin).canRemoveGroupMember(petersGroup, peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCanRemoveMember() {
        boolean actual = Policy.ForUser(peter).canRemoveGroupMember(petersGroup, klaus);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminCanRemoveMemberFromForeignGroup() {
        boolean actual = Policy.ForUser(admin).canRemoveGroupMember(petersGroup, klaus);
        assertThat(actual).isTrue();
    }

    @Test
    public void memberCannotRemoveOtherMembersFromGroup() {
        boolean actual = Policy.ForUser(klaus).canRemoveGroupMember(petersGroup, adminTwo);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonMemberCannotRemoveOtherMembersFromGroup() {
        boolean actual = Policy.ForUser(horst).canRemoveGroupMember(petersGroup, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonMemberCannotRemoveOwnerFromGroup() {
        boolean actual = Policy.ForUser(horst).canRemoveGroupMember(petersGroup, peter);
        assertThat(actual).isFalse();
    }

    /*
        Generally adding a user to a Group
     */
    @Test
    public void ownerCanGenerallyAddGroupMember() {
        boolean actual = Policy.ForUser(peter).canGenerallyAddGroupMember(petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminMemberCanGenerallyAddGroupMember() {
        boolean actual = Policy.ForUser(adminTwo).canGenerallyAddGroupMember(petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminNonMemberCanGenerallyAddGroupMember() {
        boolean actual = Policy.ForUser(admin).canGenerallyAddGroupMember(petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void memberCannotGenerallyAddGroupMember() {
        boolean actual = Policy.ForUser(klaus).canGenerallyAddGroupMember(petersGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonMemberCannotGenerallyAddGroupMember() {
        boolean actual = Policy.ForUser(rudi).canGenerallyAddGroupMember(petersGroup);
        assertThat(actual).isFalse();
    }

    /*
        Add User to Group
     */
    @Test
    public void ownerCanAddGroupMember() {
        boolean actual = Policy.ForUser(peter).canAddSpecificGroupMember(petersGroup, admin);
        assertThat(actual).isTrue();
    }

    @Test
    public void foreignAdminCanAddGroupMember() {
        boolean actual = Policy.ForUser(admin).canAddSpecificGroupMember(petersGroup, horst);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminGroupMemberCanAddGroupMember() {
        boolean actual = Policy.ForUser(adminTwo).canAddSpecificGroupMember(petersGroup, horst);
        assertThat(actual).isTrue();
    }

    @Test
    public void ownerCannotAddMemberTwice() {
        boolean actual = Policy.ForUser(peter).canAddSpecificGroupMember(petersGroup, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void foreignAdminCannotAddGroupMemberTwice() {
        boolean actual = Policy.ForUser(admin).canAddSpecificGroupMember(petersGroup, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void adminGroupMemberCannotAddGroupMemberTwice() {
        boolean actual = Policy.ForUser(adminTwo).canAddSpecificGroupMember(petersGroup, klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void groupMemberCannotAddOtherGroupMember() {
        boolean actual = Policy.ForUser(klaus).canAddSpecificGroupMember(petersGroup, horst);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonGroupMemberCannotAddOtherGroupMember() {
        boolean actual = Policy.ForUser(horst).canAddSpecificGroupMember(petersGroup, rudi);
        assertThat(actual).isFalse();
    }

    /*
        Create Group
     */
    @Test
    public void adminCanCreateGroup() {
        boolean actual = Policy.ForUser(admin).canCreateGroup();
        assertThat(actual).isTrue();
    }

    @Test
    public void userCanCreateGroup() {
        boolean actual = Policy.ForUser(peter).canCreateGroup();
        assertThat(actual).isTrue();
    }

    /*
        See Group Details (members)/group page
     */
    @Test
    public void foreignAdminCanViewGroupDetails() {
        boolean actual = Policy.ForUser(admin).canViewGroupDetails(petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminGroupMemberCanViewGroupDetails() {
        boolean actual = Policy.ForUser(adminTwo).canViewGroupDetails(petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void groupOwnerCanViewGroupDetails() {
        boolean actual = Policy.ForUser(peter).canViewGroupDetails(petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void groupMemberCanViewGroupDetails() {
        boolean actual = Policy.ForUser(klaus).canViewGroupDetails(petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void nonGroupMemberCannotViewGroupDetails() {
        boolean actual = Policy.ForUser(horst).canViewGroupDetails(petersGroup);
        assertThat(actual).isFalse();
    }

    /*
        View a list of *all* Groups
     */
    @Test
    public void adminCanViewAllGroups() {
        boolean actual = Policy.ForUser(admin).canViewAllGroupsList();
        assertThat(actual).isTrue();
    }

    @Test
    public void memberCannotViewAllGroups() {
        boolean actual = Policy.ForUser(peter).canViewAllGroupsList();
        assertThat(actual).isFalse();
    }

    /*
        Deleting a Group
     */
    @Test
    public void ownerCanDeleteGroup() {
        boolean actual = Policy.ForUser(peter).canDeleteGroup(petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void foreignAdminCanDeleteGroup() {
        boolean actual = Policy.ForUser(admin).canDeleteGroup(petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void adminGroupMemberCanDeleteGroup() {
        boolean actual = Policy.ForUser(adminTwo).canDeleteGroup(petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void groupMemberCannotDeleteGroup() {
        boolean actual = Policy.ForUser(klaus).canDeleteGroup(petersGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonGroupMemberCannotDeleteGroup() {
        boolean actual = Policy.ForUser(horst).canDeleteGroup(petersGroup);
        assertThat(actual).isFalse();
    }

    /*
        Delete Admin Group
     */
    @Test
    public void adminGroupOwnerCannotDeleteAdminGroup() {
        boolean actual = Policy.ForUser(admin).canDeleteGroup(adminGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonGroupOwningAdminCannotDeleteAdminGroup() {
        boolean actual = Policy.ForUser(adminTwo).canDeleteGroup(adminGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAdminCannotDeleteAdminGroup() {
        boolean actual = Policy.ForUser(horst).canDeleteGroup(adminGroup);
        assertThat(actual).isFalse();
    }

    /*
        Delete All Group
     */
    @Test
    public void allGroupOwnerCannotDeleteAllGroup() {
        boolean actual = Policy.ForUser(admin).canDeleteGroup(allGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonGroupOwningAdminCannotDeleteAllGroup() {
        boolean actual = Policy.ForUser(adminTwo).canDeleteGroup(allGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAdminCannotDeleteAllGroup() {
        boolean actual = Policy.ForUser(horst).canDeleteGroup(allGroup);
        assertThat(actual).isFalse();
    }

    /*
        Password reset
     */
    /* TODO: Entfernen?
    @Test
    public void adminCannotResetPassword() {
        boolean actual = Policy.ForUser(admin).canResetPassword();
        assertThat(actual).isFalse();
    }

    @Test
    public void userCanResetPassword() {
        boolean actual = Policy.ForUser().canResetPassword(peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonAuthorizedUserCanResetPassword() {
        boolean actual = Policy.ForUser().canResetPassword(null);
        assertThat(actual).isTrue();
    }
    */

    /*
        Password Update
     */
    @Test
    public void adminCannotUpdateOthersPassword() {
        boolean actual = Policy.ForUser(admin).canUpdatePassword(peter);
        assertThat(actual).isFalse();
    }

    @Test
    public void userCannotUpdateOthersPassword() {
        boolean actual = Policy.ForUser(peter).canUpdatePassword(klaus);
        assertThat(actual).isFalse();
    }

    @Test
    public void adminCanUpdateOwnPassword() {
        boolean actual = Policy.ForUser(admin).canUpdatePassword(admin);
        assertThat(actual).isTrue();
    }

    @Test
    public void userCanUpdateOwnPassword() {
        boolean actual = Policy.ForUser(peter).canUpdatePassword(peter);
        assertThat(actual).isTrue();
    }

    /*
        SessionDeletion Test
     */
    @Test
    public void adminCannotDeleteOthersSession() {
        boolean actual = Policy.ForUser(admin).canDeleteSession(petersSession);
        assertThat(actual).isFalse();
    }

    @Test
    public void userCannotDeleteOthersSession() {
        boolean actual = Policy.ForUser(klaus).canDeleteSession(petersSession);
        assertThat(actual).isFalse();
    }

    @Test
    public void userCanDeleteOwnSession() {
        boolean actual = Policy.ForUser(peter).canDeleteSession(petersSession);
        assertThat(actual).isTrue();
    }


    /*
        File Read PersmissionTest
     */
    @Test
    public void ownerCanReadFile(){
        boolean actual = Policy.ForUser(klaus).canReadFile(klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void userWithUserPermissionCanReadFile(){
        boolean actual = Policy.ForUser(peter).canReadFile(klausFile);
        assertThat(actual).isTrue();
    }
    @Test
    public void userWithGroupPermissionCanReadFile(){
        boolean actual = Policy.ForUser(klaus).canReadFile(petersGroupFile);
        assertThat(actual).isTrue();

    }
    @Test
    public void userWithGroupPermissionWithoutReadTrueCantReadFile(){
        boolean actual = Policy.ForUser(admin).canReadFile(petersGroupFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void normalUserCantReadFile(){
        boolean actual = Policy.ForUser(horst).canReadFile(klausFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonOwnerCantViewFilePermissions() {
        boolean actual = Policy.ForUser(horst).canViewFilePermissions(klausFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonOwnerAdminCantViewFilePermissions() {
        boolean actual = Policy.ForUser(admin).canViewFilePermissions(klausFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCanViewFilePermissions() {
        boolean actual = Policy.ForUser(klaus).canViewFilePermissions(klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void nonOwnerCantViewUserPermission() {
        boolean actual = Policy.ForUser(horst).canViewUserPermission(klausFilePeterUserPermission);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonOwnerAdminCantViewUserPermission() {
        boolean actual = Policy.ForUser(admin).canViewUserPermission(klausFilePeterUserPermission);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCanViewUserPermission() {
        boolean actual = Policy.ForUser(klaus).canViewUserPermission(klausFilePeterUserPermission);
        assertThat(actual).isTrue();
    }


    @Test
    public void nonOwnerCantViewGroupPermission() {
        boolean actual = Policy.ForUser(horst).canViewGroupPermission(petersGroupFilePetersGroupPermission);
        assertThat(actual).isFalse();
    }

    @Test
    public void nonOwnerAdminCantViewGroupPermission() {
        boolean actual = Policy.ForUser(admin).canViewGroupPermission(petersGroupFilePetersGroupPermission);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCanViewGroupPermission() {
        boolean actual = Policy.ForUser(peter).canViewGroupPermission(petersGroupFilePetersGroupPermission);
        assertThat(actual).isTrue();
    }
    /*
     File Write PersmissionTest
  */
    @Test
    public void ownerCanWriteFile(){
        boolean actual = Policy.ForUser(klaus).canWriteFile(klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void userWithUserPermissionCanWriteFile(){
        boolean actual = Policy.ForUser(peter).canWriteFile(klausFile);
        assertThat(actual).isTrue();
    }
    @Test
    public void userWithGroupPermissionCanWriteFile(){
        boolean actual = Policy.ForUser(klaus).canWriteFile(petersGroupFile);
        assertThat(actual).isTrue();

    }
    @Test
    public void userWithGroupPermissionWithoutWriteTrueCantWriteFile(){
        boolean actual = Policy.ForUser(rudi).canWriteFile(petersGroupFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void groupPermissionWithReadTrueWithoutWriteTrueCantWriteFile(){
        boolean actual = Policy.ForUser(rudi).canWriteFile(klausFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void normalUserCantWriteFile(){
        boolean actual = Policy.ForUser(horst).canWriteFile(klausFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCanDeleteFile() {
        boolean actual = Policy.ForUser(klaus).canDeleteFile(klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void normalUserCannotDeleteFile() {
        boolean actual = Policy.ForUser(horst).canDeleteFile(klausFile);
        assertThat(actual).isFalse();
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
        assertThat(Policy.ForUser(user).canDeleteGroupPermission(null)).isFalse();
        assertThat(Policy.ForUser(user).canDeleteGroupPermission(permission)).isFalse();
        when(file.getOwner()).thenReturn(user);
        assertThat(Policy.ForUser(user).canDeleteGroupPermission(permission)).isTrue();
    }

    @Test
    public void CanDeleteUserPermissionTest() {
        User otherUser = mock(User.class);
        User user = mock(User.class);
        File file = mock(File.class);
        UserPermission permission = mock(UserPermission.class);
        when(file.getOwner()).thenReturn(otherUser);
        when(permission.getFile()).thenReturn(file);
        assertThat(Policy.ForUser(user).canDeleteUserPermission(null)).isFalse();
        assertThat(Policy.ForUser(user).canDeleteUserPermission(permission)).isFalse();
        when(file.getOwner()).thenReturn(user);
        assertThat(Policy.ForUser(user).canDeleteUserPermission(permission)).isTrue();
    }

    @Test
    public void CanEditUserPermissionTest() {
        User otherUser = mock(User.class);
        User user = mock(User.class);
        File file = mock(File.class);
        UserPermission permission = mock(UserPermission.class);
        when(file.getOwner()).thenReturn(otherUser);
        when(permission.getFile()).thenReturn(file);
        assertThat(Policy.ForUser(user).canEditUserPermission(null)).isFalse();
        assertThat(Policy.ForUser(user).canEditUserPermission(permission)).isFalse();
        when(file.getOwner()).thenReturn(user);
        assertThat(Policy.ForUser(user).canEditUserPermission(permission)).isTrue();
    }

    @Test
    public void CanEditGroupPermissionTest() {
        User otherUser = mock(User.class);
        User user = mock(User.class);
        File file = mock(File.class);
        GroupPermission permission = mock(GroupPermission.class);
        when(file.getOwner()).thenReturn(otherUser);
        when(permission.getFile()).thenReturn(file);
        assertThat(Policy.ForUser(user).canEditGroupPermission(null)).isFalse();
        assertThat(Policy.ForUser(user).canEditGroupPermission(permission)).isFalse();
        when(file.getOwner()).thenReturn(user);
        assertThat(Policy.ForUser(user).canEditGroupPermission(permission)).isTrue();
    }
}
