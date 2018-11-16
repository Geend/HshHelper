package models.finders;

import io.ebean.Finder;
import models.UserPermission;

import java.util.List;
import java.util.Optional;

public class UserPermissionFinder extends Finder<Long, UserPermission> {
    public UserPermissionFinder() {
        super(UserPermission.class);
    }

    public List<UserPermission> findForFileId(Long fileId)
    {
        return this.query().where().eq("file_id", fileId).findList();
    }

    public Optional<UserPermission> byIdOptional(Long id) {
        UserPermission p = this.byId(id);
        if (p == null) {
            return Optional.empty();
        }
        return Optional.of(p);
    }
}
