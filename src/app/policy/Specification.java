package policy;

import models.Group;
import models.User;
import policy.session.UserSession;

public class Specification {
    public static boolean CanViewGroupDetails(User currentUser, Group toBeWatched) {
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
        if(userToBeDeleted.getGroups().stream().anyMatch(x -> x.getOwner().equals(userToBeDeleted) && x.getIsAdminGroup())) {
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

    public static boolean CanRemoveGroupMember(User currentUser, Group group, User toBeDeleted) {
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

    public static boolean isAllowedToAddGroupMember(User currentUser, Group group) {
        if(currentUser.isAdmin()) {
            return true;
        }

        if(group.getOwner().equals(currentUser)) {
            return true;
        }

        return false;
    }

    public static boolean CanAddGroupMember(User currentUser, Group group, User toBeAdded) {
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
