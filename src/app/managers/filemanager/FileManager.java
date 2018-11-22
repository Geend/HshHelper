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
        List<UserPermissionDto> userPermissions = this.userFinder
                .all()
                .stream()
                .map(x -> new UserPermissionDto(x.getUserId(), x.getUsername(), PermissionLevel.NONE))
                .collect(Collectors.toList());
        return userPermissions;
    }

    private void checkQuota(User user, String filename, String comment, byte[] data) throws QuotaExceededException {
        UserQuota uq = fileFinder.getUsedQuota(user.getUserId());
        uq.addFile(filename, comment, data);

        if (user.getQuotaLimit() <= uq.getTotalUsage()) {
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

            checkQuota(currentUser, filename, comment, new byte[]{});

            File file = new File();
            file.setOwner(currentUser);
            file.setComment(comment);
            file.setName(filename);
            file.setData(data);
            this.ebeanServer.save(file);

            for(GroupPermissionDto groupDto: initialGroupPermissions) {
                Group g = this.groupFinder.byId(groupDto.getGroupId());
                if(!this.policy.CanCreateGroupPermission(file, currentUser, g)) {
                    throw new UnauthorizedException();
                }
                CanReadWrite c = PermissionLevelConverter.ToReadWrite(groupDto.getPermissionLevel());
                GroupPermission groupPermission = new GroupPermission(file, g, c.isCanRead(), c.isCanWrite());
                this.ebeanServer.save(groupPermission);
            }
            for(UserPermissionDto userDto: initialUserPermissions) {
                User u = this.userFinder.byId(userDto.getUserId());
                if(!this.policy.CanCreateUserPermission(file, currentUser)) {
                    throw new UnauthorizedException();
                }
                CanReadWrite c = PermissionLevelConverter.ToReadWrite(userDto.getPermissionLevel());
                UserPermission userPermission = new UserPermission(file, u, c.isCanRead(), c.isCanWrite());
                this.ebeanServer.save(userPermission);
            }

            tx.commit();
        }
    }

    /*
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
    */


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
