package policy;

import models.Group;
import models.User;

public class Specification {
    public static boolean CanRemoveGroupMemeber(User currentUser, Group group, User toBeDeleted) {
        if(group.ownerId == toBeDeleted) {
            return false;
        }

        // TODO: Wenn user admin auch erlaubt. Ist jetzt zu komplex. Ebean user inkludiert groups, admin check wäre einfach.
        // TODO: Prüfen ob user in der gruppe ist. Auch zu komplex. Ebean inkludiert members.
        if(currentUser == group.ownerId) {
            return true;
        }

        return true;
    }
}
