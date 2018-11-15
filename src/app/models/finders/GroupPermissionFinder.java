package models.finders;

import io.ebean.Finder;
import models.GroupPermission;

import java.util.List;

public class GroupPermissionFinder extends Finder<Long, GroupPermission> {
    public GroupPermissionFinder() {
        super(GroupPermission.class);
    }

    public List<GroupPermission> findForFileId(Long fileId)
    {
        return this.query().where().eq("file_id", fileId).findList();
    }
}
