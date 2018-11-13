package domainlogic.filemanager;

import domainlogic.groupmanager.GroupNameAlreadyExistsException;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import models.File;
import models.Group;
import models.User;
import models.finders.FileFinder;
import models.finders.UserFinder;
import models.finders.UserQuota;

import javax.inject.Inject;
import java.util.Optional;

public class FileManager {
    private FileFinder fileFinder;
    private UserFinder userFinder;
    private final EbeanServer ebeanServer;

    @Inject
    public FileManager(FileFinder fileFinder, UserFinder userFinder, EbeanServer ebeanServer) {
        this.fileFinder = fileFinder;
        this.userFinder = userFinder;
        this.ebeanServer = ebeanServer;
    }

    public File createFile(Long userId, String filename, String comment, byte[] filedata) throws QuotaExceededException, FilenameAlreadyExistsException {
        try(Transaction tx = ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            Optional<User> user = userFinder.byIdOptional(userId);
            if(!user.isPresent()) {
                throw new IllegalArgumentException("Uid doesn't exist");
            }

            UserQuota uq = fileFinder.getUsedQuota(userId);
            uq.addFile(filename, comment, filedata);

            if(user.get().getQuotaLimit() <= uq.getTotalUsage()) {
                throw new QuotaExceededException();
            }

            Optional<File> existingFile = fileFinder.byFileName(userId, filename);
            if(existingFile.isPresent()) {
                throw new FilenameAlreadyExistsException();
            }

            File file = new File();
            file.setOwner(user.get());
            file.setComment(comment);
            file.setName(filename);
            file.setData(filedata);
            file.save();

            tx.commit();

            return file;
        }
    }
}
