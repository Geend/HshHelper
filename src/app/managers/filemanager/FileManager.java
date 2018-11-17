package managers.filemanager;

import io.ebeaninternal.server.grammer.antlr.EQLParser;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import io.ebean.*;
import io.ebean.annotation.TxIsolation;
import models.*;
import models.finders.FileFinder;
import models.finders.TempFileFinder;
import models.finders.UserFinder;
import models.finders.UserQuota;
import policyenforcement.Policy;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class FileManager {
    private FileFinder fileFinder;
    private TempFileFinder tempFileFinder;
    private UserFinder userFinder;
    private final EbeanServer ebeanServer;
    private Policy policy;

    @Inject
    public FileManager(FileFinder fileFinder, TempFileFinder tempFileFinder, UserFinder userFinder, EbeanServer ebeanServer, Policy policy) {
        this.fileFinder = fileFinder;
        this.tempFileFinder = tempFileFinder;
        this.userFinder = userFinder;
        this.ebeanServer = ebeanServer;
        this.policy = policy;
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

    public File storeFile(Long userId, Long tempFileId, String filename, String comment) throws QuotaExceededException, FilenameAlreadyExistsException, UnauthorizedException {
        try (Transaction tx = ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            Optional<User> user = userFinder.byIdOptional(userId);
            if(!user.isPresent()) {
                throw new IllegalArgumentException("Uid doesn't exist");
            }

            Optional<TempFile> tempFile = tempFileFinder.byIdOptional(tempFileId);
            if(!tempFile.isPresent()){
                throw new IllegalArgumentException("TempFile doesn't exist");
            }

            if(!policy.CanAccessTempFile(user.get(), tempFile.get())) {
                throw new UnauthorizedException();
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

    public List<File> ownedFiles(Long userId) {
        return fileFinder.getFilesByOwner(userId);
    }

    public List<File> accessibleFiles(Long userId) {
        return fileFinder.query()
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

    public UserQuota getCurrentQuotaUsage(Long userId) {
        return fileFinder.getUsedQuota(userId);
    }

    public File getFile(User currentUser, long fileId) throws InvalidArgumentException, UnauthorizedException {

        Optional<File> file = fileFinder.byIdOptional(fileId);

        if(!file.isPresent())
            throw new InvalidArgumentException();


        if(!policy.CanReadFile(currentUser, file.get()))
            throw new UnauthorizedException();

        return file.get();
    }
}
