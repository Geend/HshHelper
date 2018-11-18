package managers.filemanager;

import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import io.ebean.*;
import io.ebean.annotation.TxIsolation;
import managers.groupmanager.GroupManager;
import models.*;
import models.finders.FileFinder;
import models.finders.TempFileFinder;
import models.finders.UserFinder;
import models.finders.UserQuota;
import policyenforcement.Policy;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class FileManager {
    private final FileFinder fileFinder;
    private final TempFileFinder tempFileFinder;
    private final UserFinder userFinder;
    private final EbeanServer ebeanServer;
    private final Policy policy;
    private final SessionManager sessionManager;

    @Inject
    public FileManager(FileFinder fileFinder, TempFileFinder tempFileFinder, UserFinder userFinder, EbeanServer ebeanServer, Policy policy, SessionManager sessionManager) {
        this.fileFinder = fileFinder;
        this.tempFileFinder = tempFileFinder;
        this.userFinder = userFinder;
        this.ebeanServer = ebeanServer;
        this.policy = policy;
        this.sessionManager = sessionManager;
    }

    private void checkQuota(User user, String filename, String comment, byte[] data) throws QuotaExceededException {
        UserQuota uq = fileFinder.getUsedQuota(user.getUserId());
        uq.addFile(filename, comment, data);

        if(user.getQuotaLimit() <= uq.getTotalUsage()) {
            throw new QuotaExceededException();
        }
    }

    public TempFile createTempFile(byte[] filedata) throws QuotaExceededException {
        try(Transaction tx = ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            User user = sessionManager.currentUser();

            checkQuota(user, "", "", filedata);

            TempFile tf = new TempFile(
                user,
                filedata
            );

            tf.save();
            tx.commit();

            return tf;
        }
    }

    public File storeFile(Long tempFileId, String filename, String comment) throws QuotaExceededException, FilenameAlreadyExistsException, UnauthorizedException {
        try (Transaction tx = ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            User user = sessionManager.currentUser();

            Optional<TempFile> tempFile = tempFileFinder.byIdOptional(tempFileId);
            if(!tempFile.isPresent()){
                throw new IllegalArgumentException("TempFile doesn't exist");
            }

            if(!policy.CanAccessTempFile(user, tempFile.get())) {
                throw new UnauthorizedException();
            }

            checkQuota(user, filename, comment, new byte[]{});

            Optional<File> existingFile = fileFinder.byFileName(user.getUserId(), filename);
            if(existingFile.isPresent()) {
                throw new FilenameAlreadyExistsException();
            }

            File file = new File();
            file.setOwner(user);
            file.setComment(comment);
            file.setName(filename);
            file.setData(tempFile.get().getData());
            file.save();

            tempFile.get().delete();

            tx.commit();

            return file;
        }
    }

    public List<File> ownedFiles() {
        User user = sessionManager.currentUser();
        return fileFinder.getFilesByOwner(user.getUserId());
    }

    public List<File> getGroupFiles(Group group) throws UnauthorizedException, InvalidArgumentException {
        return fileFinder.query()
                .where()
                .and()
                .eq("groupPermissions.group", group)
                .or()
                    .eq("groupPermissions.canRead", true)
                    .eq("groupPermissions.canWrite", true)
                .endOr()
                .endAnd()
                .findList();
    }

    public List<File> accessibleFiles() {
        User user = sessionManager.currentUser();

        return fileFinder.query()
                .where()
                .or()
                    .eq("owner", user)
                    .and()
                        .eq("userPermissions.user", user)
                            .or()
                                .eq("userPermissions.canRead", true)
                                .eq("userPermissions.canWrite", true)
                            .endOr()
                    .endAnd()
                    .and()
                        .eq("groupPermissions.group.members", user)
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

    public UserQuota getCurrentQuotaUsage() {
        User user = sessionManager.currentUser();
        return fileFinder.getUsedQuota(user.getUserId());
    }

    public File getFile(long fileId) throws InvalidArgumentException, UnauthorizedException {

        Optional<File> file = fileFinder.byIdOptional(fileId);

        if(!file.isPresent())
            throw new InvalidArgumentException();


        if(!policy.CanReadFile(sessionManager.currentUser(), file.get()))
            throw new UnauthorizedException();

        return file.get();
    }


    public void editFile(User currentUser, Long fileId, String comment, byte[] data) throws InvalidArgumentException, UnauthorizedException, QuotaExceededException {

        Optional<File> fileOptional = fileFinder.byIdOptional(fileId);

        if(!fileOptional.isPresent())
            throw new InvalidArgumentException();

        if(!policy.CanWriteFile(currentUser, fileOptional.get()))
            throw new UnauthorizedException();


        File file = fileOptional.get();

        file.setComment(comment);

        if(data != null) {
            file.setData(data);
        }


        checkQuota(currentUser, file.getName(), file.getComment(), file.getData());

        ebeanServer.save(file);
    }

    public void editFile(User currentUser, Long fileId, String comment) throws QuotaExceededException, UnauthorizedException, InvalidArgumentException {
        editFile(currentUser, fileId, comment, null);
    }
}
