package domainlogic.filemanager;

import domainlogic.groupmanager.GroupNameAlreadyExistsException;
import io.ebean.*;
import io.ebean.annotation.TxIsolation;
import models.*;
import models.finders.FileFinder;
import models.finders.TempFileFinder;
import models.finders.UserFinder;
import models.finders.UserQuota;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class FileManager {
    private FileFinder fileFinder;
    private TempFileFinder tempFileFinder;
    private UserFinder userFinder;
    private final EbeanServer ebeanServer;

    @Inject
    public FileManager(FileFinder fileFinder, TempFileFinder tempFileFinder, UserFinder userFinder, EbeanServer ebeanServer) {
        this.fileFinder = fileFinder;
        this.tempFileFinder = tempFileFinder;
        this.userFinder = userFinder;
        this.ebeanServer = ebeanServer;
    }

    private void checkQuota(User user, String filename, String comment, byte[] data) throws QuotaExceededException {
        UserQuota uq = fileFinder.getUsedQuota(user.getUserId());
        uq.addFile(filename, comment, data);

        if(user.getQuotaLimit() <= uq.getTotalUsage()) {
            throw new QuotaExceededException();
        }
    }

    public TempFile createTempFile(Long userId, byte[] filedata) throws QuotaExceededException {
        try(Transaction tx = ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            Optional<User> user = userFinder.byIdOptional(userId);
            if(!user.isPresent()) {
                throw new IllegalArgumentException("Uid doesn't exist");
            }

            checkQuota(user.get(), "", "", filedata);

            TempFile tf = new TempFile(
                user.get(),
                filedata
            );

            tf.save();
            tx.commit();

            return tf;
        }
    }

    public File storeFile(Long userId, Long tempFileId, String filename, String comment) throws QuotaExceededException, FilenameAlreadyExistsException {
        try (Transaction tx = ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            Optional<User> user = userFinder.byIdOptional(userId);
            if(!user.isPresent()) {
                throw new IllegalArgumentException("Uid doesn't exist");
            }

            Optional<TempFile> tempFile = tempFileFinder.byIdOptional(tempFileId);
            if(!tempFile.isPresent()){
                throw new IllegalArgumentException("TempFile doesn't exist");
            }

            checkQuota(user.get(), filename, comment, new byte[]{});

            Optional<File> existingFile = fileFinder.byFileName(userId, filename);
            if(existingFile.isPresent()) {
                throw new FilenameAlreadyExistsException();
            }

            File file = new File();
            file.setOwner(user.get());
            file.setComment(comment);
            file.setName(filename);
            file.setData(tempFile.get().getData());
            file.save();

            tempFile.get().delete();

            tx.commit();

            return file;
        }
    }


    public File createFile(Long userId, String filename, String comment, byte[] filedata) throws QuotaExceededException, FilenameAlreadyExistsException {
        try(Transaction tx = ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            Optional<User> user = userFinder.byIdOptional(userId);
            if(!user.isPresent()) {
                throw new IllegalArgumentException("Uid doesn't exist");
            }

            checkQuota(user.get(), filename, comment, filedata);

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

    public List<File> ownedFiles(Long userId) {
        return fileFinder.getFilesByOwner(userId);
    }

    public List<File> accessibleFiles(Long userId) {
        return fileFinder.query()
                //.fetch("userPermissions")
                .fetch("userPermissions.user")
                .where()
                .or()
                    .eq("owner.userId", userId)
                    .and()
                        .eq("userPermissions.user.userId", userId)
                            .or()
                                .eq("userPermissions.canRead", true)
                                .eq("userPermissions.canWrite", true)
                            .endOr()
                    .endAnd()
                    .and()
                        .eq("groupPermissions.group.members.userId", userId)
                        .and()
                            .or()
                                .eq("groupPermissions.canRead", true)
                                .eq("groupPermissions.canWrite", true)
                            .endOr()
                        .endAnd()
                    .endAnd()
                .endOr()
                .findList();
    }
}
