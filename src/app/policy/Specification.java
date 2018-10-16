package policy;

import models.Group;
import models.User;

public class Specification {
    public static boolean CanRemoveGroup(User currentUser, Group group) {
        if(currentUser.id.equals(group.owner.id)) {
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
}
