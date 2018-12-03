package managers.permissionmanager;

import dtos.permissions.EditGroupPermissionDto;
import dtos.permissions.EditUserPermissionDto;
import extension.CanReadWrite;
import extension.PermissionLevelConverter;
import extension.logging.DangerousCharFilteringLogger;
import io.ebean.EbeanServer;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.filemanager.FileManager;
import managers.filemanager.dto.FileMeta;
import models.*;
import models.finders.*;
import play.Logger;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PermissionManager {
    private final SessionManager sessionManager;
    private final UserPermissionFinder userPermissionFinder;
    private final GroupPermissionFinder groupPermissionFinder;
    private final FileFinder fileFinder;
    private final GroupFinder groupFinder;
    private final UserFinder userFinder;
    private final EbeanServer ebeanServer;
    private final String requestErrorMessage;
    private final FileManager fileManager;

    private static final Logger.ALogger logger = new DangerousCharFilteringLogger(PermissionManager.class);

    @Inject
    public PermissionManager(
            UserPermissionFinder userPermissionFinder,
            GroupPermissionFinder groupPermissionFinder,
            FileFinder fileFinder,
            GroupFinder groupFinder,
            UserFinder userFinder,
            EbeanServer ebeanServer,
            SessionManager sessionManager, FileManager fileManager) {
        this.sessionManager = sessionManager;
        this.userPermissionFinder = userPermissionFinder;
        this.groupPermissionFinder = groupPermissionFinder;
        this.fileFinder = fileFinder;
        this.groupFinder = groupFinder;
        this.userFinder = userFinder;
        this.ebeanServer = ebeanServer;
        this.fileManager = fileManager;
        this.requestErrorMessage = "Fehler bei der Verarbeitung der Anfrage. Haben sie ungültige Informationen eingegeben?";
    }

    //
    //  group permissions
    //

    public void editGroupPermission(Long groupPermissionId, PermissionLevel newLevel) throws InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<GroupPermission> permissionOpt = this.groupPermissionFinder.byIdOptional(groupPermissionId);

        if (!permissionOpt.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        GroupPermission permission = permissionOpt.get();

        if (!sessionManager.currentPolicy().canEditGroupPermission(permission)) {
            logger.error(user + " tried to change the permissions for group " + permission.getGroup() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        CanReadWrite c = PermissionLevelConverter.ToReadWrite(newLevel);
        permission.setCanWrite(c.getCanWrite());
        permission.setCanRead(c.getCanRead());

        this.ebeanServer.save(permission);
        logger.info(user+ " changed permissions for group " + permission.getGroup() + "to canWrite: " + c.getCanWrite() + " and canRead: " + c.getCanRead());
    }

    public EditGroupPermissionDto getGroupPermissionForEdit(Long groupPermissionId) throws InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<GroupPermission> permissionOpt = this.groupPermissionFinder.byIdOptional(groupPermissionId);

        if (!permissionOpt.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        GroupPermission gp = permissionOpt.get();

        if (!sessionManager.currentPolicy().canEditGroupPermission(gp)) {
            logger.error(user + " tried to edit the permissions for group " + gp.getGroup() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        return new EditGroupPermissionDto(
            gp.getGroupPermissionId(),
            PermissionLevelConverter.FromReadWrite(gp.getCanRead(), gp.getCanWrite()),
            gp.getGroup().getGroupId(),
            gp.getGroup().getName(),
            gp.getFile().getFileId(),
            gp.getFile().getName()
        );
    }

    public void deleteGroupPermission(Long groupPermissionId) throws InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<GroupPermission> permissionOpt = this.groupPermissionFinder.byIdOptional(groupPermissionId);

        if (!permissionOpt.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        GroupPermission permission = permissionOpt.get();

        if (!sessionManager.currentPolicy().canDeleteGroupPermission(permission)) {
            logger.error(user + " tried to delete permissions for group " + permission.getGroup() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        this.ebeanServer.delete(permission);
        logger.info(user + " deleted permissions for group" + permission.getGroup());
    }

    //
    // user permissions
    //

    public EditUserPermissionDto getUserPermissionForEdit(Long userPermissionId) throws InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<UserPermission> permissionOpt = this.userPermissionFinder.byIdOptional(userPermissionId);

        if (!permissionOpt.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        UserPermission permission = permissionOpt.get();

        if (!sessionManager.currentPolicy().canEditUserPermission(permission)) {
            logger.error(user + " tried to edit permissions for user " + permission.getUser() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        PermissionLevel permissionLevel = PermissionLevelConverter.FromReadWrite(permission.getCanRead(), permission.getCanWrite());
        List<PermissionLevel> possiblePermissions = Arrays.asList(PermissionLevel.values());

        return new EditUserPermissionDto(
                permission.getUserPermissionId(),
                permissionLevel, possiblePermissions,
                permission.getUser().getUsername(),
                permission.getFile().getFileId(),
                permission.getFile().getName()
        );
    }

    public void deleteUserPermission(Long userPermissionId) throws InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<UserPermission> permissionOpt = this.userPermissionFinder.byIdOptional(userPermissionId);

        if (!permissionOpt.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        UserPermission permission = permissionOpt.get();

        if (!sessionManager.currentPolicy().canDeleteUserPermission(permission)) {
            logger.error(user + " tried to delete permissions for user " + permission.getUser() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        this.ebeanServer.delete(permission);
        logger.info(user + " deleted permissions for user" + permission.getUser());
    }

    public void editUserPermission(Long userPermissionId, PermissionLevel newLevel) throws InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<UserPermission> permissionOpt = this.userPermissionFinder.byIdOptional(userPermissionId);

        if (!permissionOpt.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        UserPermission permission = permissionOpt.get();

        if (!sessionManager.currentPolicy().canEditUserPermission(permission)) {
            logger.error(user + " tried to change the permissions for user " + permission.getUser() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        CanReadWrite c = PermissionLevelConverter.ToReadWrite(newLevel);
        permission.setCanWrite(c.getCanWrite());
        permission.setCanRead(c.getCanRead());
        this.ebeanServer.save(permission);
        logger.info(user + " changed permissions for user " + permission.getUser() + "to canWrite: " + c.getCanWrite() + " and canRead: " + c.getCanRead());
    }

    //
    //  create permissions
    //

    public void createUserPermission(Long fileId, Long userId, PermissionLevel permissionLevel) throws InvalidArgumentException, UnauthorizedException {
        User currentUser = this.sessionManager.currentUser();
        Optional<File> fileOpt = fileFinder.byIdOptional(fileId);
        Optional<User> userOpt = userFinder.byIdOptional(userId);

        if (!userOpt.isPresent())
            throw new InvalidArgumentException(this.requestErrorMessage);

        if (!fileOpt.isPresent())
            throw new InvalidArgumentException(this.requestErrorMessage);

        User user = userOpt.get();
        File file = fileOpt.get();

        if (!sessionManager.currentPolicy().canCreateUserPermission(file)) {
            logger.error(currentUser + " tried to create permissions for file " + file + " but he is not authorized");
            throw new UnauthorizedException();
        }

        boolean userAlreadyHasPermission = userPermissionFinder.findExistingPermissionForUser(fileId, user).size() > 0;
        if(userAlreadyHasPermission)
            throw new InvalidArgumentException("Es existiert bereits eine Berechtigung");

        UserPermission permission = new UserPermission();
        permission.setFile(file);
        permission.setUser(user);

        CanReadWrite c = PermissionLevelConverter.ToReadWrite(permissionLevel);
        permission.setCanWrite(c.getCanWrite());
        permission.setCanRead(c.getCanRead());

        ebeanServer.save(permission);
        logger.info(currentUser + " created new permissions for user " + user +  " on file " + file);
    }

    public void createGroupPermission(Long fileId, Long groupId, PermissionLevel permissionLevel) throws InvalidArgumentException, UnauthorizedException {
        User currentUser = this.sessionManager.currentUser();
        Optional<File> fileOpt = fileFinder.byIdOptional(fileId);
        Optional<Group> groupOpt = groupFinder.byIdOptional(groupId);

        if (!groupOpt.isPresent())
            throw new InvalidArgumentException(this.requestErrorMessage);

        if (!fileOpt.isPresent())
            throw new InvalidArgumentException(this.requestErrorMessage);

        Group group = groupOpt.get();
        File file = fileOpt.get();

        if (!sessionManager.currentPolicy().canCreateGroupPermission(file, group)) {
            logger.error(currentUser + " tried to create permissions for group " + group + " for file " + file + " but he is not authorized");
            throw new UnauthorizedException();
        }

        boolean groupAlreadyHasPermission = groupPermissionFinder.findExistingPermissionForGroup(fileId, group).size() > 0;
        if(groupAlreadyHasPermission)
            throw new InvalidArgumentException("Es existiert bereits eine Berechtigung");


        GroupPermission permission = new GroupPermission();
        permission.setFile(file);
        permission.setGroup(group);

        CanReadWrite c = PermissionLevelConverter.ToReadWrite(permissionLevel);
        permission.setCanWrite(c.getCanWrite());
        permission.setCanRead(c.getCanRead());

        ebeanServer.save(permission);
        logger.info(currentUser + " created new permissions for group " + group + " on file " + file);
    }

    public List<User> getAllOtherUsers(Long userId) {
        return userFinder.findAllButThis(userId);
    }

    public FileMeta getFileMeta(Long fileId) throws UnauthorizedException, InvalidArgumentException {
        return fileManager.getFileMeta(fileId);
    }
}
