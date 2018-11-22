package models.finders;

import io.ebean.Finder;
import io.ebean.SqlQuery;
import io.ebean.SqlRow;
import models.File;
import models.User;
import play.Logger;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class FileFinder extends Finder<Long, File> {

    public FileFinder() {
        super(File.class);
    }

    public Optional<File> byIdOptional(Long id) {
        File f = this.byId(id);
        if (f == null) {
            return Optional.empty();
        }
        return Optional.of(f);
    }

    public Optional<File> byFileName(Long ownerId, String fileName) {
        return this.query().where()
                .eq("name", fileName)
                .eq("owner_id", ownerId)
                .findOneOrEmpty();
    }

    public Set<File> byUserHasUserPermission(User user){
        return this.query()
                .where()
                .eq("userPermissions.user", user)
                .or()
                    .eq("userPermissions.canRead", true)
                    .eq("userPermissions.canWrite", true)
                .endOr()
                .findSet();
    }

    public Set<File> byUserHasGroupPermission(User user){
        return this.query()
                .where()
                .eq("groupPermissions.group.members", user)
                .or()
                    .eq("groupPermissions.canRead", true)
                    .eq("groupPermissions.canWrite", true)
                .endOr()
                .findSet();
    }

    public UserQuota getUsedQuota(Long userId) {
        SqlQuery qry = this.db().createSqlQuery(
            "SELECT IFNULL(SUM(LENGTH(name)), ZERO()) AS nameSum, IFNULL(SUM(LENGTH(comment)), ZERO()) AS commentSum, IFNULL(SUM(LENGTH(data)), ZERO()) AS dataSum FROM files "+
            "WHERE owner_id = :owner_id"
        );
        qry.setParameter("owner_id", userId);
        SqlRow filesRow = qry.findOne();

        UserQuota quota = new UserQuota();
        quota.setNameUsage(filesRow.getLong("nameSum"));
        quota.setCommentUsage(filesRow.getLong("commentSum"));
        quota.setFileContentUsage(filesRow.getLong("dataSum"));


        return quota;
    }

    public List<File> getFilesByOwner(Long userId) {
        return this.query().where().eq("owner_id", userId).findList();
    }

}