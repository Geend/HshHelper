package policy;

import models.*;
import policy.session.Session;


import java.util.Optional;

public class Specification {
    public static Specification instance = new Specification();

    public boolean CanViewGroupDetails(User currentUser, Group toBeWatched) {
        if(currentUser == null) {
            return false;
        }

        if(toBeWatched.getMembers().contains(currentUser)) {
            return true;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public boolean CanViewAllUsers(User currentUser) {
        if(currentUser == null) {
            return false;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public boolean CanViewAllGroupsList(User currentUser) {
        if(currentUser == null) {
            return false;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public boolean CanCreateUser(User currentUser) {
        if(currentUser == null) {
            return false;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public boolean CanDeleteUser(User currentUser, User userToBeDeleted) {
        if(currentUser == null || userToBeDeleted == null) {
            return false;
        }

        // Wenn Nutzer Owner einer Admin-Gruppe ist, darf er nicht gelöscht werden (Quasi Super-Admin)
        if(userToBeDeleted.getGroups().stream().anyMatch(x -> x.getOwner().equals(userToBeDeleted) && x.getIsAdminGroup())) {
            return false;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }


    public boolean CanResetPassword(User currentUser) {
        if(currentUser == null) {
            return true;
        }

        return false;
    }

    public boolean CanDeleteGroup(User currentUser, Group group) {
        if(currentUser == null) {
            return false;
        }

        if(group.getIsAdminGroup() || group.getIsAllGroup()) {
            return false;
        }

        if(currentUser.equals(group.getOwner())) {
            return true;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public boolean CanRemoveGroupMember(User currentUser, Group group, User toBeDeleted) {
        if(currentUser == null) {
            return false;
        }

        if(group.getOwner().equals(toBeDeleted)) {
            return false;
        }

        if(!group.getMembers().contains(toBeDeleted)) {
            return false;
        }

        if(currentUser.equals(group.getOwner())) {
            return true;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public boolean CanGenerallyAddGroupMember(User currentUser, Group group) {
        if(currentUser.isAdmin()) {
            return true;
        }

        if(group.getOwner().equals(currentUser)) {
            return true;
        }

        return false;
    }

    public boolean CanAddSpecificGroupMember(User currentUser, Group group, User toBeAdded) {
        if(currentUser == null) {
            return false;
        }

        // No duplicates!
        if(group.getMembers().contains(toBeAdded)) {
            return false;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        if(group.getOwner().equals(currentUser)) {
            return true;
        }

        return false;
    }

    public boolean CanCreateGroup(User currentUser) {
        if(currentUser == null) {
            return false;
        }

        return true;
    }

    public boolean CanUpdatePassword(User currentUser, User toBeUpdated) {
        if(currentUser == null) {
            return false;
        }

        if(currentUser.equals(toBeUpdated)) {
            return true;
        }

        return false;
    }

    public boolean CanDeleteSession(User currentUser, Session session) {
        if(currentUser == null) {
            return false;
        }

        if(session.getUser().equals(currentUser)) {
            return true;
        }

        return false;
    }

    public boolean CanSeeAllGroups(User currentUser) {
        if(currentUser == null) {
            return false;
        }
        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }



    public boolean CanReadFile(User user, File file){
        if(user == null || file == null)
            return false;

        if(file.getOwner().equals(user))
            return true;

        if(user.getUserPermissions().stream().filter(up -> up.getFile().equals(file)).anyMatch(UserPermission::getCanRead))
            return true;

        if(user.getGroups().stream().anyMatch(group -> group.getGroupPermissions().stream().filter(groupPermission -> groupPermission.getFile().equals(file)).anyMatch(GroupPermission::getCanRead)))
            return true;


        return false;

    }

    public boolean CanWriteFile(User user, File file){
        if(user == null || file == null)
            return false;

        if(file.getOwner().equals(user))
            return true;

        if(user.getUserPermissions().stream().filter(up -> up.getFile().equals(file)).anyMatch(UserPermission::getCanWrite))
            return true;

        if(user.getGroups().stream().anyMatch(group -> group.getGroupPermissions().stream().filter(groupPermission -> groupPermission.getFile().equals(file)).anyMatch(GroupPermission::getCanWrite)))
            return true;

        return false;
    }

    public boolean CanCreateUserPermission(File file, User user) {

        if(user == null || file == null)
            return false;

        if(file.getOwner().equals(user))
            return true;

        return false;
    }

    public boolean CanCreateGroupPermission(File file, User user, Group group) {
        if(user == null || file == null || group == null)
            return false;

        if(group.getMembers().contains(user))
            return true;

        return false;
    }
}
