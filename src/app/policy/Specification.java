package policy;

import models.Group;
import models.User;
import play.Logger;

public class Specification {
    public static boolean CanViewGroupDetails(User currentUser, Group toBeWatched) {
        if(toBeWatched.members.contains(currentUser)) {
            return true;
        }

        return false;
    }

    public static boolean CanViewAllGroupsList(User currentUser) {
        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public static boolean CanCreateUser(User currentUser) {
        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public static boolean CanRemoveUser(User currentUser) {
        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public static boolean CanChangePassword(User currentUser, User toBeUpdated) {
        if(currentUser.equals(toBeUpdated)) {
            return true;
        }
        
        return false;
    }

    public static boolean CanResetPassword(User currentUser, User toBeResetted) {
        return true;
    }

    public static boolean CanRemoveGroup(User currentUser, Group group) {
        if(currentUser.equals(group.owner)) {
            return true;
        }

        if(currentUser.isAdmin()) {
            return true;
        }

        return false;
    }

    public static boolean CanRemoveGroupMemeber(User currentUser, Group group, User toBeDeleted) {
        if(group.owner.equals(toBeDeleted)) {
            return false;
        }

        if(!group.members.contains(toBeDeleted)) {
            return false;
        }

        if(currentUser.equals(group.owner)) {
            return true;
        }

        return false;
    }

    public static boolean CanAddGroupMember(User currentUser, Group group, User toBeAdded) {
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
        return true;
    }
}
