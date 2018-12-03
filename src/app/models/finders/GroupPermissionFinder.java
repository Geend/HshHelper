package models.finders;

import io.ebean.Finder;
import models.Group;
import models.GroupPermission;

import java.util.List;
import java.util.Optional;

public class GroupPermissionFinder extends Finder<Long, GroupPermission> {
    public GroupPermissionFinder() {
        super(GroupPermission.class);
    }

    public List<GroupPermission> findExistingPermissionForGroup(Long fileId, Group group)
    {
        return this.query().where()
                .eq("fk_file_id", fileId)
                .and()
                .eq("group.groupId", group.getGroupId())
                .findList();
    }

    public Optional<GroupPermission> byIdOptional(Long id) {
        GroupPermission p = this.byId(id);
        if (p == null) {
            return Optional.empty();
        }
        return Optional.of(p);
    }
}
