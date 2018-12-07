import models.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import policyenforcement.Policy;
import policyenforcement.session.Session;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PolicyTests {

    private User admin;
    private User adminTwo;
    private User peter;
    private User klaus;
    private User horst;
    private User rudi;
    private Group allGroup;
    private Group adminGroup;
    private Group petersGroup;
    private Group klausGroup;
    private Session petersSession;

    private File klausFile;
    private UserPermission klausFilePeterUserPermission;
    private GroupPermission klausFileKlausGroupPermission;

    private File petersGroupFile;
    private GroupPermission petersGroupFilePetersGroupPermission;
    private GroupPermission petersGroupFileAdminGroupPermission;

    /*
        adminGroup:
            admin, adminTwo

        petersGroup:
            peter, klaus, adminTwo

        no *explicit* groups:
            horst, rudi
     */
    @Before
    public void setup() {
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
        klausFile = new File("klausFile", "Klaus Comment", data, klaus);
        klausFilePeterUserPermission = new UserPermission(klausFile, peter, true, true);
        peter.getUserPermissions().add(klausFilePeterUserPermission);


        petersGroupFile = new File("petersGroupFile", "Peters Group File", data, peter);
        petersGroupFilePetersGroupPermission = new GroupPermission(petersGroupFile, petersGroup, true, true);
        petersGroup.getGroupPermissions().add(petersGroupFilePetersGroupPermission);

        petersGroupFileAdminGroupPermission = new GroupPermission(petersGroupFile, adminGroup, false, false);
        adminGroup.getGroupPermissions().add(petersGroupFileAdminGroupPermission);


        klausFileKlausGroupPermission = new GroupPermission(petersGroupFile, adminGroup, true, false);
        klausGroup.getGroupPermissions().add(klausFileKlausGroupPermission);

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

    /*currentP
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

    /*
        View User
     */

    @Test
    public void adminCanSeeAllUsers() {
        boolean actual = Policy.ForUser(admin).canViewAllUsers();
        assertThat(actual).isTrue();
    }

    @Test
    public void normalUserCantSeeAllUsers() {
        boolean actual = Policy.ForUser(klaus).canViewAllUsers();
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
    public void ownerCanReadFile() {
        boolean actual = Policy.ForUser(klaus).canReadFile(klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void userWithUserPermissionCanReadFile() {
        boolean actual = Policy.ForUser(peter).canReadFile(klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void userWithGroupPermissionCanReadFile() {
        boolean actual = Policy.ForUser(klaus).canReadFile(petersGroupFile);
        assertThat(actual).isTrue();

    }

    @Test
    public void userWithGroupPermissionWithoutReadTrueCantReadFile() {
        boolean actual = Policy.ForUser(admin).canReadFile(petersGroupFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void normalUserCantReadFile() {
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
    public void ownerCanWriteFile() {
        boolean actual = Policy.ForUser(klaus).canWriteFile(klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void userWithUserPermissionCanWriteFile() {
        boolean actual = Policy.ForUser(peter).canWriteFile(klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void userWithGroupPermissionCanWriteFile() {
        boolean actual = Policy.ForUser(klaus).canWriteFile(petersGroupFile);
        assertThat(actual).isTrue();

    }

    @Test
    public void userWithGroupPermissionWithoutWriteTrueCantWriteFile() {
        boolean actual = Policy.ForUser(rudi).canWriteFile(petersGroupFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void groupPermissionWithReadTrueWithoutWriteTrueCantWriteFile() {
        boolean actual = Policy.ForUser(rudi).canWriteFile(klausFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void normalUserCantWriteFile() {
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
        View file meta
     */
    @Test
    public void ownerCanGetFileMeta() {
        boolean actual = Policy.ForUser(klaus).canGetFileMeta(klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void readPermissionUserCnaGetFileMeta() {
        rudi.getUserPermissions().add(new UserPermission(klausFile, rudi, true, false));
        boolean actual = Policy.ForUser(rudi).canGetFileMeta(klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void writePermissionUserCanGetFileMeta() {
        rudi.getUserPermissions().add(new UserPermission(klausFile, rudi, false, true));
        boolean actual = Policy.ForUser(rudi).canGetFileMeta(klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void readwritePermissionUserCanGetFileMeta() {
        rudi.getUserPermissions().add(new UserPermission(klausFile, rudi, true, true));
        boolean actual = Policy.ForUser(rudi).canGetFileMeta(klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void noPermissionUserCantGetFileMeta() {
        boolean actual = Policy.ForUser(rudi).canGetFileMeta(klausFile);
        assertThat(actual).isFalse();
    }

    /*
        Zugriff auf Berechtigungen
     */

    @Test
    public void canDeleteGroupPermissionTest() {
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
    public void canDeleteUserPermissionTest() {
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
    public void canEditUserPermissionTest() {
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
    public void canEditGroupPermissionTest() {
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


    @Test
    public void ownerCanCreateUserPermission() {
        boolean actual = Policy.ForUser(klaus).canCreateUserPermission(klausFile);
        assertThat(actual).isTrue();
    }

    @Test
    public void notOwnerCantCreateUserPermission() {
        boolean actual = Policy.ForUser(peter).canCreateUserPermission(klausFile);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCanCreateGroupPermission() {
        boolean actual = Policy.ForUser(klaus).canCreateGroupPermission(klausFile, petersGroup);
        assertThat(actual).isTrue();
    }

    @Test
    public void notOwnerCantCreateGroupPermission() {
        boolean actual = Policy.ForUser(peter).canCreateGroupPermission(klausFile, allGroup);
        assertThat(actual).isFalse();
    }

    @Test
    public void ownerCantCreateGroupPermissionForForeignGroup() {
        boolean actual = Policy.ForUser(klaus).canCreateGroupPermission(klausFile, adminGroup);
        assertThat(actual).isFalse();
    }


    @Test
    public void adminCanViewUserMetaInfo() {
        boolean actual = Policy.ForUser(admin).canViewUserMetaInfo();
        assertThat(actual).isTrue();
    }

    @Test
    public void normalUserCantViewUserMetaInfo() {
        boolean actual = Policy.ForUser(klaus).canViewUserMetaInfo();
        assertThat(actual).isFalse();
    }


    /*
        UserSettings
     */
    @Test
    public void selfCanChangeTimeoutValue() {
        assertThat(Policy.ForUser(rudi).canChangeUserTimeoutValue(rudi)).isTrue();
    }

    @Test
    public void foreignCantChangeTimeoutValue() {
        assertThat(Policy.ForUser(rudi).canChangeUserTimeoutValue(peter)).isFalse();
    }


    /*
        Quota
    */
    @Test
    public void adminCanReadWriteQuotaLimit() {
        boolean actual = Policy.ForUser(admin).canReadWriteQuotaLimit();
        assertThat(actual).isTrue();
    }

    @Test
    public void normalUserCantReadWriteQuotaLimit() {
        boolean actual = Policy.ForUser(klaus).canReadWriteQuotaLimit();
        assertThat(actual).isFalse();
    }

    /*
        NetService
     */

    @Test
    public void adminCanSeeAllNetServices() {
        assertThat(Policy.ForUser(admin).canSeeAllNetServices()).isTrue();
    }

    @Test
    public void normalUserCanSeeAllNetServices() {
        assertThat(Policy.ForUser(horst).canSeeAllNetServices()).isTrue();
    }

    @Test
    public void adminCanSeeNetServiceOverviewPage() {
        assertThat(Policy.ForUser(admin).canSeeNetServiceOverviewPage()).isTrue();
    }

    @Test
    public void normalUserCantSeeNetServiceOverviewPage() {
        assertThat(Policy.ForUser(horst).canSeeNetServiceOverviewPage()).isFalse();
    }

    @Test
    public void adminCanCreateNetService() {
        assertThat(Policy.ForUser(admin).canCreateNetService()).isTrue();
    }

    @Test
    public void normalUserCantCreateNetService() {
        assertThat(Policy.ForUser(klaus).canCreateNetService()).isFalse();
    }

    @Test
    public void adminCanEditNetService() {
        assertThat(Policy.ForUser(admin).canEditNetService()).isTrue();
    }

    @Test
    public void normalUserCantEditNetService() {
        assertThat(Policy.ForUser(klaus).canEditNetService()).isFalse();
    }

    @Test
    public void adminCanDeleteNetService() {
        assertThat(Policy.ForUser(admin).canDeleteNetServices()).isTrue();
    }

    @Test
    public void normalUserCantDeleteNetService() {
        assertThat(Policy.ForUser(klaus).canDeleteNetServices()).isFalse();
    }

    /*
        NetService Credential
     */

    @Test
    public void selfCanReadCredential() {
        NetServiceCredential credential = mock(NetServiceCredential.class);
        when(credential.getUser()).thenReturn(rudi);
        assertThat(Policy.ForUser(rudi).canReadCredential(credential)).isTrue();
    }

    @Test
    public void foreignUserCantReadCredential() {
        NetServiceCredential credential = mock(NetServiceCredential.class);
        when(credential.getUser()).thenReturn(rudi);
        assertThat(Policy.ForUser(peter).canReadCredential(credential)).isFalse();
    }

    @Test
    public void selfCanDeleteCredential() {
        NetServiceCredential credential = mock(NetServiceCredential.class);
        when(credential.getUser()).thenReturn(rudi);
        assertThat(Policy.ForUser(rudi).canDeleteNetServicesCredential(credential)).isTrue();
    }

    @Test
    public void foreignUserCantDeleteCredential() {
        NetServiceCredential credential = mock(NetServiceCredential.class);
        when(credential.getUser()).thenReturn(rudi);
        assertThat(Policy.ForUser(peter).canDeleteNetServicesCredential(credential)).isFalse();
    }



    /*
        2FA
     */

    @Test
    public void selfCanDisable2FA() {
        assertThat(Policy.ForUser(klaus).canDisable2FA(klaus)).isTrue();
    }

    @Test
    public void adminCanDisable2FA() {
        assertThat(Policy.ForUser(admin).canDisable2FA(klaus)).isTrue();

    }

    @Test
    public void foreignUserCantDisable2FA() {
        assertThat(Policy.ForUser(rudi).canDisable2FA(klaus)).isFalse();

    }



    /*
        Null parameter Checks
     */

    @Test
    public void canViewGroupDetailsNullTest() {
        assertThat(Policy.ForUser(admin).canViewGroupDetails(null)).isFalse();
    }

    @Test
    public void canDeleteUserNullTest() {
        assertThat(Policy.ForUser(admin).canDeleteUser(null)).isFalse();
    }

    @Test
    public void canDeleteGroupNullTest() {
        assertThat(Policy.ForUser(admin).canDeleteGroup(null)).isFalse();
    }

    @Test
    public void canRemoveGroupMemberNullTest() {
        assertThat(Policy.ForUser(admin).canRemoveGroupMember(null, null)).isFalse();
        assertThat(Policy.ForUser(admin).canRemoveGroupMember(mock(Group.class), null)).isFalse();
        assertThat(Policy.ForUser(admin).canRemoveGroupMember(null, mock(User.class))).isFalse();

    }

    @Test
    public void canGenerallyAddGroupMemberTest() {
        assertThat(Policy.ForUser(admin).canGenerallyAddGroupMember(null)).isFalse();
    }

    @Test
    public void canAddSpecificGroupMemberNullTest() {
        assertThat(Policy.ForUser(admin).canAddSpecificGroupMember(null, null)).isFalse();
        assertThat(Policy.ForUser(admin).canAddSpecificGroupMember(mock(Group.class), null)).isFalse();
        assertThat(Policy.ForUser(admin).canAddSpecificGroupMember(null, mock(User.class))).isFalse();

    }

    @Test
    public void canUpdatePasswordNullTest() {
        assertThat(Policy.ForUser(admin).canUpdatePassword(null)).isFalse();
    }

    @Test
    public void canDeleteSessionNullTest() {
        assertThat(Policy.ForUser(admin).canDeleteSession(null)).isFalse();
    }

    @Test
    public void canReadFileNullTest() {
        assertThat(Policy.ForUser(admin).canReadFile(null)).isFalse();
    }

    @Test
    public void canWriteFileNullTest() {
        assertThat(Policy.ForUser(admin).canWriteFile(null)).isFalse();
    }

    @Test
    public void canGetFileMetaNullTest() {
        assertThat(Policy.ForUser(admin).canGetFileMeta(null)).isFalse();
    }

    @Test
    public void canDeleteFileNullTest() {
        assertThat(Policy.ForUser(admin).canDeleteFile(null)).isFalse();
    }

    @Test
    public void canDeleteGroupPermissionNullTest() {
        assertThat(Policy.ForUser(admin).canDeleteGroupPermission(null)).isFalse();
    }

    @Test
    public void canDeleteUserPermissionNullTest() {
        assertThat(Policy.ForUser(admin).canDeleteUserPermission(null)).isFalse();
    }

    @Test
    public void canEditUserPermissionNullTest() {
        assertThat(Policy.ForUser(admin).canEditUserPermission(null)).isFalse();
    }

    @Test
    public void canEditGroupPermissionNullTest() {
        assertThat(Policy.ForUser(admin).canEditGroupPermission(null)).isFalse();
    }

    @Test
    public void canCreateUserPermissionNullTest() {
        assertThat(Policy.ForUser(admin).canCreateUserPermission(null)).isFalse();
    }

    @Test
    public void canCreateGroupPermissionNullTest() {
        assertThat(Policy.ForUser(admin).canCreateGroupPermission(null, null)).isFalse();
        assertThat(Policy.ForUser(admin).canCreateGroupPermission(mock(File.class), null)).isFalse();
        assertThat(Policy.ForUser(admin).canCreateGroupPermission(null, mock(Group.class))).isFalse();

    }

    @Test
    public void canViewFilePermissionsNullTest() {
        assertThat(Policy.ForUser(admin).canViewFilePermissions(null)).isFalse();
    }

    @Test
    public void canViewUserPermissionNullTest() {
        assertThat(Policy.ForUser(admin).canViewUserPermission(null)).isFalse();
    }

    @Test
    public void canViewGroupPermissionNullTest() {
        assertThat(Policy.ForUser(admin).canViewGroupPermission(null)).isFalse();
    }

    @Test
    public void canChangeUserTimeoutValueNullTest() {
        assertThat(Policy.ForUser(admin).canChangeUserTimeoutValue(null)).isFalse();
    }

    @Test
    public void canReadCredentialNullTest() {
        assertThat(Policy.ForUser(admin).canReadCredential(null)).isFalse();
    }

    @Test
    public void canDeleteNetServicesCredentialNullTest() {
        assertThat(Policy.ForUser(admin).canDeleteNetServicesCredential(null)).isFalse();
    }

    @Test
    public void canDisable2FANullTest() {
        assertThat(Policy.ForUser(admin).canDisable2FA(null)).isFalse();
    }

}
