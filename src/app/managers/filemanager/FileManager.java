package managers.filemanager;

import dtos.file.GroupPermissionDto;
import dtos.file.UserPermissionDto;
import extension.CanReadWrite;
import extension.PermissionLevelConverter;
import io.ebean.annotation.TxIsolation;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import io.ebean.*;
import managers.filemanager.dto.FileMeta;
import models.*;
import models.finders.FileFinder;
import models.finders.GroupFinder;
import models.finders.UserFinder;
import models.finders.UserQuota;
import org.joda.time.DateTime;
import play.Logger;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

public class FileManager {
    private final GroupFinder groupFinder;
    private final FileFinder fileFinder;
    private final UserFinder userFinder;
    private final EbeanServer ebeanServer;
    private final SessionManager sessionManager;
    private final FileMetaFactory fileMetaFactory;

    private static final Logger.ALogger logger = Logger.of(FileManager.class);

    @Inject
    public FileManager(FileFinder fileFinder, UserFinder userFinder, EbeanServer ebeanServer, SessionManager sessionManager, GroupFinder groupFinder, FileMetaFactory fileMetaFactory) {
        this.groupFinder = groupFinder;
        this.fileFinder = fileFinder;
        this.userFinder = userFinder;
        this.ebeanServer = ebeanServer;
        this.sessionManager = sessionManager;
        this.fileMetaFactory = fileMetaFactory;
    }

    public List<GroupPermissionDto> getGroupPermissionDtosForCreate() {
        User currentUser = this.sessionManager.currentUser();
        List<GroupPermissionDto> groupPermissions = currentUser.getGroups()
                .stream()
                .map(x -> new GroupPermissionDto(x.getGroupId(), x.getName(), PermissionLevel.NONE))
                .collect(Collectors.toList());
        return groupPermissions;
    }

    public List<UserPermissionDto> getUserPermissionDtosForCreate() {
        User currentUser = this.sessionManager.currentUser();
        List<UserPermissionDto> userPermissions = this.userFinder
                .findAllButThis(currentUser.getUserId())
                .stream()
                .map(x -> new UserPermissionDto(x.getUserId(), x.getUsername(), PermissionLevel.NONE))
                .collect(Collectors.toList());
        return userPermissions;
    }

    private void checkQuota(User user) throws QuotaExceededException {
        UserQuota uq = fileFinder.getUsedQuota(user.getUserId());
        if (user.getQuotaLimit() <= uq.getTotalUsage()) {
            logger.error(user + " tried to exceed quota.");
            throw new QuotaExceededException();
        }
    }

    public void createFile(
            String filename,
            String comment,
            byte[] data,
            List<UserPermissionDto> initialUserPermissions,
            List<GroupPermissionDto> initialGroupPermissions) throws FilenameAlreadyExistsException, QuotaExceededException, UnauthorizedException {
        User currentUser = this.sessionManager.currentUser();
        try (Transaction tx = ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            Optional<File> existingFile = fileFinder.byFileName(currentUser.getUserId(), filename);
            if (existingFile.isPresent()) {
                throw new FilenameAlreadyExistsException();
            }

            File file = new File();
            file.setOwner(currentUser);
            file.setComment(comment);
            file.setName(filename);
            file.setData(data);
            file.setWrittenBy(currentUser);
            file.setWrittenByDt(DateTime.now());
            this.ebeanServer.save(file);

            this.checkQuota(currentUser);

            for(GroupPermissionDto groupDto: initialGroupPermissions) {
                Group g = this.groupFinder.byId(groupDto.getGroupId());
                if(!sessionManager.currentPolicy().canCreateGroupPermission(file, g)) {
                    throw new UnauthorizedException();
                }
                CanReadWrite c = PermissionLevelConverter.ToReadWrite(groupDto.getPermissionLevel());
                GroupPermission groupPermission = new GroupPermission(file, g, c.getCanRead(), c.getCanWrite());
                this.ebeanServer.save(groupPermission);
            }

            for(UserPermissionDto userDto: initialUserPermissions) {
                User u = this.userFinder.byId(userDto.getUserId());
                if(!sessionManager.currentPolicy().canCreateUserPermission(file)) {
                    throw new UnauthorizedException();
                }
                CanReadWrite c = PermissionLevelConverter.ToReadWrite(userDto.getPermissionLevel());
                UserPermission userPermission = new UserPermission(file, u, c.getCanRead(), c.getCanWrite());
                this.ebeanServer.save(userPermission);
            }

            tx.commit();
            logger.info(currentUser + " added file " + file);
        }
    }

    public List<FileMeta> accessibleFiles() {
        User user = sessionManager.currentUser();

        Set<File> result = new HashSet<>();
        result.addAll(user.getOwnedFiles());
        result.addAll(fileFinder.byUserHasUserPermission(user));
        result.addAll(fileFinder.byUserHasGroupPermission(user));

        return fileMetaFactory.fromFiles(new ArrayList<>(result));
    }

    public List<FileMeta> sharedWithCurrentUserFiles() {
        User user = sessionManager.currentUser();

        Set<File> result = new HashSet<>();
        result.addAll(fileFinder.byUserHasUserPermission(user));
        result.addAll(fileFinder.byUserHasGroupPermission(user));
        result.removeAll(user.getOwnedFiles());

        return fileMetaFactory.fromFiles(new ArrayList<>(result));
    }

    public List<FileMeta> sharedByCurrentUserFiles() {
        User user = sessionManager.currentUser();
        List<File> files = user.getOwnedFiles().stream().filter(x -> x.getGroupPermissions().size() > 0 || x.getUserPermissions().size() > 0).collect(Collectors.toList());
        return fileMetaFactory.fromFiles(files);
    }

    public List<FileMeta> ownedByCurrentUserFiles() {
        User user = sessionManager.currentUser();
        return fileMetaFactory.fromFiles(user.getOwnedFiles());
    }

    public UserQuota getCurrentQuotaUsage() {
        User user = sessionManager.currentUser();
        return fileFinder.getUsedQuota(user.getUserId());
    }

    public FileMeta getFileMeta(long fileId) throws InvalidArgumentException, UnauthorizedException {
        Optional<File> optFile = fileFinder.byIdOptional(fileId);

        if (!optFile.isPresent()) {
            throw new InvalidArgumentException();
        }

        File file = optFile.get();

        if(!sessionManager.currentPolicy().canGetFileMeta(file)) {
            logger.error(sessionManager.currentUser() + " tried to access file " + file + " meta info but he is not authorized");
            throw new UnauthorizedException();
        }

        return fileMetaFactory.fromFile(file);
    }

    public byte[] getFileContent(long fileId) throws UnauthorizedException, InvalidArgumentException {
        Optional<File> file = fileFinder.byIdOptional(fileId);

        if (!file.isPresent())
            throw new InvalidArgumentException();

        if (!sessionManager.currentPolicy().canReadFile(file.get())) {
            logger.error(sessionManager.currentUser() + "  tried to access file " + file.get() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        logger.info(sessionManager.currentUser() + " is accessing file " + file.get());
        return file.get().getData();
    }

    public void deleteFile(long fileId) throws UnauthorizedException, InvalidArgumentException {
        User user = sessionManager.currentUser();

        Optional<File> optFile = fileFinder.byIdOptional(fileId);

        if (!optFile.isPresent())
            throw new InvalidArgumentException();

        File file = optFile.get();

        if (!sessionManager.currentPolicy().canDeleteFile(file)) {
            logger.error(user + " tried to delete file " + file + " but he is not authorized");
            throw new UnauthorizedException("Du bist nicht autorisiert, diese Datei zu löschen.");
        }

        ebeanServer.delete(file);
        logger.info(user + " deleted file " + file);
    }

    public void editFileContent(Long fileId, byte[] data) throws QuotaExceededException, UnauthorizedException, InvalidArgumentException {
        User user = sessionManager.currentUser();

        try (Transaction tx = ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            Optional<File> fileOptional = fileFinder.byIdOptional(fileId);

            if (!fileOptional.isPresent())
                throw new InvalidArgumentException();

            if (!sessionManager.currentPolicy().canWriteFile(fileOptional.get())) {
                logger.error(user + " tried to change the content file " + fileOptional.get() + " but he is not authorized");
                throw new UnauthorizedException();
            }

            File file = fileOptional.get();
            file.setData(data);
            file.setWrittenBy(user);
            file.setWrittenByDt(DateTime.now());
            ebeanServer.save(file);

            // Wir müssen die Quota vom Owner checken, nicht die vom aktuellen user.
            // Nur quota vom owner wird erhöht!
            this.checkQuota(file.getOwner());

            tx.commit();
            logger.info(user + " has changed the content of file " + file);
        }
    }

    public void editFileComment(Long fileId, String comment) throws InvalidArgumentException, UnauthorizedException, QuotaExceededException {
        User user = sessionManager.currentUser();

        try (Transaction tx = ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            Optional<File> fileOptional = fileFinder.byIdOptional(fileId);

            if (!fileOptional.isPresent())
                throw new InvalidArgumentException();

            if (!sessionManager.currentPolicy().canWriteFile(fileOptional.get())) {
                logger.error(user + " tried to change the comment of file " + fileOptional.get() + " but he is not authorized");
                throw new UnauthorizedException();
            }

            File file = fileOptional.get();
            file.setComment(comment);
            ebeanServer.save(file);

            // Wir müssen die Quota vom Owner checken, nicht die vom aktuellen user.
            // Nur quota vom owner wird erhöht!
            this.checkQuota(file.getOwner());

            tx.commit();
            logger.info(user + " has changed the comment of file " + file);
        }
    }

    public List<FileMeta> searchFile(String query) {
        return accessibleFiles().stream().filter(x -> like(x.getFilename(), query)).collect(Collectors.toList());
    }

    private boolean like(String str, String expr) {
        str = str.toLowerCase();
        return str.startsWith(expr) ||
                str.endsWith(expr) ||
                str.contains(expr);
    }
}
