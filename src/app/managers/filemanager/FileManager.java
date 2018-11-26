package managers.filemanager;

import dtos.GroupPermissionDto;
import dtos.UserPermissionDto;
import extension.CanReadWrite;
import extension.PermissionLevelConverter;
import io.ebean.annotation.TxIsolation;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import io.ebean.*;
import models.*;
import models.finders.FileFinder;
import models.finders.GroupFinder;
import models.finders.UserFinder;
import models.finders.UserQuota;
import org.joda.time.DateTime;
import play.Logger;
import policyenforcement.Policy;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

public class FileManager {
    private final GroupFinder groupFinder;
    private final FileFinder fileFinder;
    private final UserFinder userFinder;
    private final EbeanServer ebeanServer;
    private final Policy policy;
    private final SessionManager sessionManager;

    private static final Logger.ALogger logger = Logger.of(FileManager.class);

    @Inject
    public FileManager(FileFinder fileFinder, UserFinder userFinder, EbeanServer ebeanServer, Policy policy, SessionManager sessionManager, GroupFinder groupFinder) {
        this.groupFinder = groupFinder;
        this.fileFinder = fileFinder;
        this.userFinder = userFinder;
        this.ebeanServer = ebeanServer;
        this.policy = policy;
        this.sessionManager = sessionManager;
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
            logger.error(user.getUsername() + " tried to exceed quota");
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
                if(!this.policy.CanCreateGroupPermission(file, currentUser, g)) {
                    throw new UnauthorizedException();
                }
                CanReadWrite c = PermissionLevelConverter.ToReadWrite(groupDto.getPermissionLevel());
                GroupPermission groupPermission = new GroupPermission(file, g, c.getCanRead(), c.getCanWrite());
                this.ebeanServer.save(groupPermission);
            }
            for(UserPermissionDto userDto: initialUserPermissions) {
                User u = this.userFinder.byId(userDto.getUserId());
                if(!this.policy.CanCreateUserPermission(file, currentUser)) {
                    throw new UnauthorizedException();
                }
                CanReadWrite c = PermissionLevelConverter.ToReadWrite(userDto.getPermissionLevel());
                UserPermission userPermission = new UserPermission(file, u, c.getCanRead(), c.getCanWrite());
                this.ebeanServer.save(userPermission);
            }

            tx.commit();
        }
        logger.info(currentUser.getUsername() + " added a file");
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


        if (!policy.CanReadFile(sessionManager.currentUser(), file.get())) {
            logger.error(sessionManager.currentUser().getUsername() + " tried to access file " + file.get().getName() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        logger.info(sessionManager.currentUser().getUsername() + " is accessing file " + file.get().getName());
        return file.get();
    }

    public void deleteFile(long fileId) throws UnauthorizedException, InvalidArgumentException {
        User user = sessionManager.currentUser();
        File file = getFile(fileId);

        if (!policy.CanDeleteFile(user, file)) {
            logger.error(user.getUsername() + " tried to delete file " + file.getName() + " but he is not authorized");
            throw new UnauthorizedException("Du bist nicht autorisiert, diese Datei zu löschen.");
        }

        ebeanServer.delete(file);
        logger.info(user.getUsername() + " deleted file " + file.getName());
    }

    public void editFileContent(Long fileId, byte[] data) throws QuotaExceededException, UnauthorizedException, InvalidArgumentException {
        User user = sessionManager.currentUser();

        try (Transaction tx = ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            Optional<File> fileOptional = fileFinder.byIdOptional(fileId);

            if (!fileOptional.isPresent())
                throw new InvalidArgumentException();

            if (!policy.CanWriteFile(user, fileOptional.get())) {
                logger.error(user.getUsername() + " tried to change the content file with id " + fileId + " but he is not authorized");
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
        }

        logger.info(user.getUsername() + " has changed the content of file with id " + fileId);
    }

    public void editFileComment(Long fileId, String comment) throws InvalidArgumentException, UnauthorizedException, QuotaExceededException {
        User user = sessionManager.currentUser();

        try (Transaction tx = ebeanServer.beginTransaction(TxIsolation.SERIALIZABLE)) {
            Optional<File> fileOptional = fileFinder.byIdOptional(fileId);

            if (!fileOptional.isPresent())
                throw new InvalidArgumentException();

            if (!policy.CanWriteFile(user, fileOptional.get())) {
                logger.error(user.getUsername() + " tried to change the comment of file with id " + fileId + " but he is not authorized");
                throw new UnauthorizedException();
            }

            File file = fileOptional.get();
            file.setComment(comment);
            ebeanServer.save(file);

            // Wir müssen die Quota vom Owner checken, nicht die vom aktuellen user.
            // Nur quota vom owner wird erhöht!
            this.checkQuota(file.getOwner());

            tx.commit();
        }

        logger.info(user.getUsername() + " has changed the comment of file with id " + fileId);
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
