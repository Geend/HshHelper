package policy;

import models.Group;
import models.User;
import models.UserSession;
import play.Logger;

public class Specification {
    public static boolean CanViewGroupDetails(User currentUser, Group toBeWatched) {
        if(currentUser == null) {
            return false;
        }

        if(toBeWatched.members.contains(currentUser)) {
            return true;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public static boolean CanViewAllUsers(User currentUser) {
        if(currentUser == null) {
            return false;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public static boolean CanViewAllGroupsList(User currentUser) {
        if(currentUser == null) {
            return false;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public static boolean CanCreateUser(User currentUser) {
        if(currentUser == null) {
            return false;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public static boolean CanDeleteUser(User currentUser, User userToBeDeleted) {
        if(currentUser == null || userToBeDeleted == null) {
            return false;
        }

        // Wenn Nutzer Owner einer Admin-Gruppe ist, darf er nicht gelöscht werden (Quasi Super-Admin)
        if(userToBeDeleted.groups.stream().anyMatch(x -> x.owner.equals(userToBeDeleted) && x.isAdminGroup)) {
            return false;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public static boolean CanChangePassword(User currentUser, User toBeUpdated) {
        if(currentUser == null) {
            return false;
        }

        if(currentUser.equals(toBeUpdated)) {
            return true;
        }

        return false;
    }

    public static boolean CanResetPassword(User currentUser) {
        if(currentUser == null) {
            return true;
        }

        return false;
    }

    public static boolean CanDeleteGroup(User currentUser, Group group) {
        if(currentUser == null) {
            return false;
        }

        if(group.isAdminGroup || group.isAllGroup) {
            return false;
        }

        if(currentUser.equals(group.owner)) {
            return true;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public static boolean CanRemoveGroupMember(User currentUser, Group group, User toBeDeleted) {
        if(currentUser == null) {
            return false;
        }

        if(group.owner.equals(toBeDeleted)) {
            return false;
        }

        if(!group.members.contains(toBeDeleted)) {
            return false;
        }

        if(currentUser.equals(group.owner)) {
            return true;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public static boolean isAllowedToAddGroupMember(User currentUser, Group group) {
        if(currentUser.isAdmin()) {
            return true;
        }

        if(group.owner.equals(currentUser)) {
            return true;
        }

        return false;
    }

    public static boolean CanAddGroupMember(User currentUser, Group group, User toBeAdded) {
        if(currentUser == null) {
            return false;
        }

        // No duplicates!
        if(group.members.contains(toBeAdded)) {
            return false;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        if(group.owner.equals(currentUser)) {
            return true;
        }

        return false;
    }

    public static boolean CanCreateGroup(User currentUser) {
        if(currentUser == null) {
            return false;
        }

        return true;
    }

    public static boolean CanUpdatePassword(User currentUser, User toBeUpdated) {
        if(currentUser == null) {
            return false;
        }

        if(currentUser.equals(toBeUpdated)) {
            return true;
        }

        return false;
    }

    public static boolean CanDeleteSession(User currentUser, UserSession session) {
        if(currentUser == null) {
            return false;
        }

        if(session.getUser().equals(currentUser)) {
            return true;
        }

        return false;
    }
}
