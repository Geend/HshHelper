package models.finders;

import io.ebean.Finder;
import models.UserPermission;

import java.util.List;

public class UserPermissionFinder extends Finder<Long, UserPermission> {
    public UserPermissionFinder() {
        super(UserPermission.class);
    }

    public List<UserPermission> findForFileId(Long fileId)
    {
        return this.query().where().eq("file_id", fileId).findList();
    }
}
