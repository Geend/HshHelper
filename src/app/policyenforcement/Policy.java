package policyenforcement;

import models.*;
import policyenforcement.session.Session;

public class Policy {
    public static Policy ForUser(User user) {
        return new Policy(user);
    }

    //-----

    private User associatedUser;

    private Policy(User associatedUser) {
        if (associatedUser == null) {
            throw new IllegalArgumentException("associatedUser cannot be null!");
        }

        this.associatedUser = associatedUser;
    }

    public boolean canViewGroupDetails(Group toBeWatched) {
        if (toBeWatched.getMembers().contains(associatedUser)) {
            return true;
        }

        if (associatedUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public boolean canViewAllUsers() {
        if (associatedUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public boolean canViewAllGroupsList() {
        if (associatedUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public boolean canCreateUser() {
        if (associatedUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public boolean canDeleteUser(User userToBeDeleted) {
        if (userToBeDeleted == null) {
            return false;
        }

        // Wenn Nutzer Owner einer Admin-Gruppe ist, darf er nicht gelöscht werden (Quasi Super-Admin)
        if (userToBeDeleted.getGroups().stream().anyMatch(x -> x.getOwner().equals(userToBeDeleted) && x.getIsAdminGroup())) {
            return false;
        }

        if (associatedUser.isAdmin()) {
            return true;
        }

        return false;
    }


    public boolean canDeleteGroup(Group group) {
        if (group.getIsAdminGroup() || group.getIsAllGroup()) {
            return false;
        }

        if (associatedUser.equals(group.getOwner())) {
            return true;
        }

        if (associatedUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public boolean canRemoveGroupMember(Group group, User toBeDeleted) {
        //Can't remove from the "all" group, because every user needs te be a member of it
        if (group.getIsAllGroup()) {
            return false;
        }

        if (group.getOwner().equals(toBeDeleted)) {
            return false;
        }

        if (!group.getMembers().contains(toBeDeleted)) {
            return false;
        }

        if (associatedUser.equals(group.getOwner())) {
            return true;
        }

        if (associatedUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public boolean canGenerallyAddGroupMember(Group group) {
        // Can't add to all group -> managed by the system
        if (group.getIsAllGroup()) {
            return false;
        }

        if (associatedUser.isAdmin()) {
            return true;
        }

        if (group.getOwner().equals(associatedUser)) {
            return true;
        }

        return false;
    }

    public boolean canAddSpecificGroupMember(Group group, User toBeAdded) {
        //Can't add to the "all" group -> managed by the system
        if (group.getIsAllGroup()) {
            return false;
        }

        // No duplicates!
        if (group.getMembers().contains(toBeAdded)) {
            return false;
        }

        if (associatedUser.isAdmin()) {
            return true;
        }

        if (group.getOwner().equals(associatedUser)) {
            return true;
        }

        return false;
    }

    public boolean canCreateGroup() {
        return true;
    }

    public boolean canUpdatePassword(User toBeUpdated) {
        if (associatedUser.equals(toBeUpdated)) {
            return true;
        }

        return false;
    }

    public boolean canDeleteSession(Session session) {
        if (session.getUser().equals(associatedUser)) {
            return true;
        }

        return false;
    }

    public boolean canReadFile(File file) {
        if (file == null)
            return false;

        if (file.getOwner().equals(associatedUser))
            return true;

        if (associatedUser.getUserPermissions().stream().filter(up -> up.getFile().equals(file)).anyMatch(UserPermission::getCanRead))
            return true;

        if (associatedUser.getGroups().stream().anyMatch(group -> group.getGroupPermissions().stream().filter(groupPermission -> groupPermission.getFile().equals(file)).anyMatch(GroupPermission::getCanRead)))
            return true;

        return false;

    }

    public boolean canWriteFile(File file) {
        if (file == null)
            return false;

        if (file.getOwner().equals(associatedUser))
            return true;

        if (associatedUser.getUserPermissions().stream().filter(up -> up.getFile().equals(file)).anyMatch(UserPermission::getCanWrite))
            return true;

        if (associatedUser.getGroups().stream().anyMatch(group -> group.getGroupPermissions().stream().filter(groupPermission -> groupPermission.getFile().equals(file)).anyMatch(GroupPermission::getCanWrite)))
            return true;

        return false;
    }

    public boolean canGetFileMeta(File file) {
        if (canReadFile(file))
            return true;

        if (canWriteFile(file))
            return true;

        return false;
    }

    public boolean canDeleteFile(File file) {
        if (file == null)
            return false;

        if (file.getOwner().equals(associatedUser))
            return true;

        return false;
    }

    public boolean canDeleteGroupPermission(GroupPermission groupPermission) {
        if (groupPermission == null)
            return false;

        if (associatedUser.equals(groupPermission.getFile().getOwner()))
            return true;


        return false;
    }

    public boolean canDeleteUserPermission(UserPermission userPermission) {
        if (userPermission == null)
            return false;

        if (associatedUser.equals(userPermission.getFile().getOwner()))
            return true;

        return false;
    }

    public boolean canEditUserPermission(UserPermission userPermission) {
        if (userPermission == null)
            return false;

        if (associatedUser.equals(userPermission.getFile().getOwner()))
            return true;

        return false;
    }

    public boolean canEditGroupPermission(GroupPermission userPermission) {
        if (userPermission == null)
            return false;

        if (associatedUser.equals(userPermission.getFile().getOwner()))
            return true;

        return false;
    }

    public boolean canCreateUserPermission(File file) {
        if (file == null)
            return false;

        if (file.getOwner().equals(associatedUser))
            return true;

        return false;
    }

    public boolean canCreateGroupPermission(File file, Group group) {
        if (file == null || group == null)
            return false;

        if (group.getMembers().contains(associatedUser) && file.getOwner().equals(associatedUser))
            return true;

        return false;
    }

    public boolean canViewFilePermissions(File file) {
        if (file.getOwner().equals(associatedUser))
            return true;

        return false;
    }

    public boolean canViewUserPermission(UserPermission permission) {
        if (permission.getFile().getOwner().equals(associatedUser))
            return true;

        return false;
    }

    public boolean canViewGroupPermission(GroupPermission permission) {
        if (permission.getFile().getOwner().equals(associatedUser))
            return true;

        return false;
    }

    public boolean canViewUserMetaInfo() {
        if (associatedUser.isAdmin())
            return true;

        return false;
    }

    public boolean canChangeUserTimeoutValue(User toBeChanged) {
        if (associatedUser.equals(toBeChanged))
            return true;

        return false;
    }

    public boolean canReadWriteQuotaLimit() {
        if (associatedUser.isAdmin())
            return true;

        return false;
    }


    public boolean canSeeAllNetServices() {
        return true;
    }

    public boolean canSeeNetServiceOverviewPage(){
        if (associatedUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public boolean canCreateNetService() {
        if (associatedUser.isAdmin()) {
            return true;
        }

        return false;
    }


    public boolean canEditNetService() {
        if (associatedUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public boolean canDeleteNetServices() {
        if (associatedUser.isAdmin()) {
            return true;
        }

        return false;
    }


    public boolean canReadCredential(NetServiceCredential netServiceCredential) {
        if(netServiceCredential.getUser().equals(associatedUser)) {
            return true;
        }

        return false;
    }

    public boolean canDeleteNetServicesCredential(NetServiceCredential netServiceCredential) {
        if (netServiceCredential.getUser().equals(associatedUser)) {
            return true;
        }

        return false;
    }



    public boolean canDisable2FA(User toDeDisabled) {
        if(toDeDisabled.equals(associatedUser))
            return true;

        if(associatedUser.isAdmin())
            return true;

        return false;
    }
}
