package models.finders;

import io.ebean.Finder;
import io.ebean.SqlQuery;
import io.ebean.SqlRow;
import models.File;
import models.User;

import java.util.List;
import java.util.Optional;

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

    public UserQuota getUsedQuota(Long userId) {
        SqlQuery qry = this.db().createSqlQuery(
            "SELECT SUM(LENGTH(name)) AS nameSum, SUM(LENGTH(comment)) AS commentSum, SUM(LENGTH(data)) AS dataSum FROM files "+
            "WHERE owner_id = :owner_id"
        );
        qry.setParameter("owner_id", userId);
        SqlRow row = qry.findOne();


        UserQuota quota = new UserQuota();
        quota.setNameUsage(row.getLong("nameSum"));
        quota.setCommentUsage(row.getLong("commentSum"));
        quota.setFileContentUsage(row.getLong("dataSum"));

        return quota;
    }

    public List<File> getFilesByOwner(Long userId) {
        return this.query().where().eq("owner_id", userId).findList();
    }
}