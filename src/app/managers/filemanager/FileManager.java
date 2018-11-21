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
import java.util.*;
import java.util.stream.Collectors;

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

        if (user.getQuotaLimit() <= uq.getTotalUsage()) {
            throw new QuotaExceededException();
        }
    }

    public TempFile createTempFile(byte[] filedata) throws QuotaExceededException {
        try (Transaction tx = ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
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
            if (!tempFile.isPresent()) {
                throw new IllegalArgumentException("TempFile doesn't exist");
            }

            if (!policy.CanAccessTempFile(user, tempFile.get())) {
                throw new UnauthorizedException();
            }

            checkQuota(user, filename, comment, new byte[]{});

            Optional<File> existingFile = fileFinder.byFileName(user.getUserId(), filename);
            if (existingFile.isPresent()) {
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


    public List<File> accessibleFiles() {
        User user = sessionManager.currentUser();

        Set<File> result = new HashSet<>();
        result.addAll(user.getOwnedFiles());
        result.addAll(fileFinder.byUserHasUserPermission(user));
        result.addAll(fileFinder.byUserHasGroupPermission(user));

        return new ArrayList<>(result);

    }

    public List<File> sharedWithCurrentUserFiles() {
        User user = sessionManager.currentUser();

        Set<File> result = new HashSet<>();
        result.addAll(fileFinder.byUserHasUserPermission(user));
        result.addAll(fileFinder.byUserHasGroupPermission(user));
        result.removeAll(user.getOwnedFiles());

        return new ArrayList<>(result);
    }


    public UserQuota getCurrentQuotaUsage() {
        User user = sessionManager.currentUser();
        return fileFinder.getUsedQuota(user.getUserId());
    }

    public File getFile(long fileId) throws InvalidArgumentException, UnauthorizedException {

        Optional<File> file = fileFinder.byIdOptional(fileId);

        if (!file.isPresent())
            throw new InvalidArgumentException();


        if (!policy.CanReadFile(sessionManager.currentUser(), file.get()))
            throw new UnauthorizedException();

        return file.get();
    }

    public void deleteFile(long fileId) throws UnauthorizedException, InvalidArgumentException {
        User user = sessionManager.currentUser();
        File file = getFile(fileId);

        // TODO: implement policy check -> can delete file
        ebeanServer.delete(file);
    }


    public void editFile(Long fileId, String comment, byte[] data) throws InvalidArgumentException, UnauthorizedException, QuotaExceededException {
        User user = sessionManager.currentUser();

        Optional<File> fileOptional = fileFinder.byIdOptional(fileId);

        if (!fileOptional.isPresent())
            throw new InvalidArgumentException();

        if (!policy.CanWriteFile(user, fileOptional.get()))
            throw new UnauthorizedException();


        File file = fileOptional.get();

        file.setComment(comment);

        if (data != null) {
            file.setData(data);
        }


        checkQuota(user, file.getName(), file.getComment(), file.getData());

        ebeanServer.save(file);
    }

    public void editFile(Long fileId, String comment) throws QuotaExceededException, UnauthorizedException, InvalidArgumentException {
        editFile(fileId, comment, null);
    }

    public void removeTempFiles() {
        User user = sessionManager.currentUser();
        List<TempFile> tempFiles = tempFileFinder.getFilesByOwner(user.getUserId());

        tempFiles.forEach(ebeanServer::delete);

    }

    public List<File> searchFile(String query) {
        return accessibleFiles().stream().filter(x -> like(x.getName(), query)).collect(Collectors.toList());
    }

    private boolean like(String str, String expr) {
        str = str.toLowerCase();
        return str.startsWith(expr) ||
                str.endsWith(expr) ||
                str.contains(expr);
    }
}
