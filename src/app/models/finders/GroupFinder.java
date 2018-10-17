package models.finders;

import io.ebean.Finder;
import models.Group;

import java.util.List;
import java.util.Optional;

public class GroupFinder extends Finder<Long, Group> {

    /**
     * Construct using the default EbeanServer.
     */
    public GroupFinder() {
        super(Group.class);
    }

    public Optional<Group> byIdOptional(Long id) {
        return Optional.of(this.byId(id));
    }
}