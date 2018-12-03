package models.finders;

import io.ebean.Finder;
import models.User;
import models.UserPermission;

import java.util.List;
import java.util.Optional;

public class UserPermissionFinder extends Finder<Long, UserPermission> {
    public UserPermissionFinder() {
        super(UserPermission.class);
    }

    public List<UserPermission> findExistingPermissionForUser(Long fileId, User user)
    {
        return this.query().where()
                .eq("fk_file_id", fileId)
                .and()
                .eq("user.userId", user.getUserId())
                .findList();
    }

    public Optional<UserPermission> byIdOptional(Long id) {
        UserPermission p = this.byId(id);
        if (p == null) {
            return Optional.empty();
        }
        return Optional.of(p);
    }
}
