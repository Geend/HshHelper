package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Group {

    public int id;
    public String name;
    public int ownerId;
    public boolean isAdminGroup;

    public Group() {

    }

    public Group(int id, String name, int ownerId, boolean isAdminGroup) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.isAdminGroup = isAdminGroup;
    }

    // note: only testcode for the first day, switch later to in memory
    // database h2
    private static List<Group> groups;

    static {
        groups = new ArrayList<Group>();
        groups.add(new Group(0, "Administrator", 0, true));
        groups.add(new Group(1, "Alle", 0, false));
        groups.add(new Group(2, "Peter ihm seine Gruppe", 1, false));
    }

    public static List<Group> findAll() {
        return new ArrayList<Group>(groups);
    }

    public static void addGroup(Group g) {
        g.id = groups.size();
        groups.add(g);
    }

    public static Group getById(int id) {
        Optional<Group> g = groups.stream().filter(x -> x.id == id).findFirst();
        return g.orElse(null);
    }
}
