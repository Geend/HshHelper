package models;

import java.util.ArrayList;
import java.util.List;

public class GroupMembership {

    public int groupId;
    public int userId;

    public GroupMembership(int groupId, int userId) {
        this.userId = userId;
        this.groupId = groupId;
    }

    // note: only testcode for the first day, switch later to in memory
    // database h2
    private static List<GroupMembership> groupMemberships;

    static {
        groupMemberships = new ArrayList<GroupMembership>();
        groupMemberships.add(new GroupMembership(0, 0));
        groupMemberships.add(new GroupMembership(1, 0));
    }

    public static List<GroupMembership> findAll() {
        return new ArrayList<GroupMembership>(groupMemberships);
    }
}
