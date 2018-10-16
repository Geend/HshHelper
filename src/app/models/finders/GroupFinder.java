package models.finders;

import io.ebean.Finder;
import models.Group;

public class GroupFinder extends Finder<Long, Group> {

    /**
     * Construct using the default EbeanServer.
     */
    public GroupFinder() {
        super(Group.class);
    }

}