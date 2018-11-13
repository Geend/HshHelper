package domainlogic.filemanager;

import io.ebean.EbeanServer;
import models.finders.FileFinder;

import javax.inject.Inject;

public class FileManager {

    private FileFinder fileFinder;
    private final EbeanServer ebeanServer;


    @Inject
    public FileManager(FileFinder fileFinder, EbeanServer ebeanServer) {
        this.fileFinder = fileFinder;
        this.ebeanServer = ebeanServer;
    }
}
