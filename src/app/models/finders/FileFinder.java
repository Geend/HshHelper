package models.finders;

import io.ebean.Finder;
import models.File;

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

    public Optional<File> byFileName(String fileName) {
        return this.query().where().eq("name", fileName).findOneOrEmpty();
    }

}