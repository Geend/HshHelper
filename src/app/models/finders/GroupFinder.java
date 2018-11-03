package models.finders;

import io.ebean.Finder;
import models.Group;

import java.util.Optional;

public class GroupFinder extends Finder<Long, Group> {

    /**
     * Construct using the default EbeanServer.
     */
    public GroupFinder() {
        super(Group.class);
    }

    public Optional<Group> byIdOptional(Long id) {

        Group group = this.byId(id);
        if (group == null) {
            return Optional.empty();
        }
        return Optional.of(group);
    }

    public Optional<Group> byName(String name) {
        return this.query().where().eq("name", name).findOneOrEmpty();
    }
}