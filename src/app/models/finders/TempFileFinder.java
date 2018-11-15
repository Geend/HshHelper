package models.finders;

import io.ebean.Finder;
import models.File;
import models.TempFile;

import java.util.Optional;

public class TempFileFinder extends Finder<Long, TempFile> {
    public TempFileFinder() {super(TempFile.class); }

    public Optional<TempFile> byIdOptional(Long id) {
        TempFile f = this.byId(id);
        if (f == null) {
            return Optional.empty();
        }
        return Optional.of(f);
    }
}
