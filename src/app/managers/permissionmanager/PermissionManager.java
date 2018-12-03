package managers.permissionmanager;

import extension.CanReadWrite;
import extension.PermissionLevelConverter;
import extension.logging.DangerousCharFilteringLogger;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import io.ebean.EbeanServer;
import managers.filemanager.FileManager;
import managers.filemanager.dto.FileMeta;
import models.*;
import models.File;
import models.GroupPermission;
import models.UserPermission;
import dtos.permissions.EditGroupPermissionDto;
import dtos.permissions.EditUserPermissionDto;
import models.finders.*;
import models.finders.FileFinder;
import models.finders.GroupPermissionFinder;
import models.finders.UserPermissionFinder;
import play.Logger;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import java.util.*;

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
        Optional<GroupPermission> permission = this.groupPermissionFinder.byIdOptional(groupPermissionId);

        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!sessionManager.currentPolicy().canEditGroupPermission(permission.get())) {
            logger.error(user + " tried to change the permissions for group " + permission.get().getGroup() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        CanReadWrite c = PermissionLevelConverter.ToReadWrite(newLevel);
        permission.get().setCanWrite(c.getCanWrite());
        permission.get().setCanRead(c.getCanRead());

        this.ebeanServer.save(permission.get());
        logger.info(user+ " changed permissions for group " + permission.get().getGroup() + "to canWrite: " + c.getCanWrite() + " and canRead: " + c.getCanRead());
    }

    public EditGroupPermissionDto getGroupPermissionForEdit(Long groupPermissionId) throws InvalidDataException, InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<GroupPermission> permission = this.groupPermissionFinder.byIdOptional(groupPermissionId);

        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!sessionManager.currentPolicy().canEditGroupPermission(permission.get())) {
            logger.error(user + " tried to edit the permissions for group " + permission.get().getGroup() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        GroupPermission gp = permission.get();

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
        Optional<GroupPermission> permission = this.groupPermissionFinder.byIdOptional(groupPermissionId);

        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!sessionManager.currentPolicy().canDeleteGroupPermission(permission.get())) {
            logger.error(user + " tried to delete permissions for group " + permission.get().getGroup() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        this.ebeanServer.delete(permission.get());
        logger.info(user + " deleted permissions for group" + permission.get().getGroup());
    }

    //
    // user permissions
    //

    public EditUserPermissionDto getUserPermissionForEdit(Long userPermissionId) throws InvalidDataException, InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<UserPermission> permission = this.userPermissionFinder.byIdOptional(userPermissionId);

        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!sessionManager.currentPolicy().canEditUserPermission(permission.get())) {
            logger.error(user + " tried to edit permissions for user " + permission.get().getUser() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        PermissionLevel permissionLevel = PermissionLevelConverter.FromReadWrite(permission.get().getCanRead(), permission.get().getCanWrite());
        List<PermissionLevel> possiblePermissions = Arrays.asList(PermissionLevel.values());

        return new EditUserPermissionDto(
                permission.get().getUserPermissionId(),
                permissionLevel, possiblePermissions,
                permission.get().getUser().getUsername(),
                permission.get().getFile().getFileId(),
                permission.get().getFile().getName()
        );
    }

    public void deleteUserPermission(Long userPermissionId) throws InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<UserPermission> permission = this.userPermissionFinder.byIdOptional(userPermissionId);

        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!sessionManager.currentPolicy().canDeleteUserPermission(permission.get())) {
            logger.error(user + " tried to delete permissions for user " + permission.get().getUser() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        this.ebeanServer.delete(permission.get());
        logger.info(user + " deleted permissions for user" + permission.get().getUser());
    }

    public void editUserPermission(Long userPermissionId, PermissionLevel newLevel) throws InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<UserPermission> permission = this.userPermissionFinder.byIdOptional(userPermissionId);

        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!sessionManager.currentPolicy().canEditUserPermission(permission.get())) {
            logger.error(user + " tried to change the permissions for user " + permission.get().getUser() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        CanReadWrite c = PermissionLevelConverter.ToReadWrite(newLevel);
        permission.get().setCanWrite(c.getCanWrite());
        permission.get().setCanRead(c.getCanRead());
        this.ebeanServer.save(permission.get());
        logger.info(user + " changed permissions for user " + permission.get().getUser() + "to canWrite: " + c.getCanWrite() + " and canRead: " + c.getCanRead());
    }

    //
    //  create permissions
    //

    public void createUserPermission(Long fileId, Long userId, PermissionLevel permissionLevel) throws InvalidArgumentException, UnauthorizedException {
        User currentUser = this.sessionManager.currentUser();
        Optional<File> file = fileFinder.byIdOptional(fileId);
        Optional<User> user = userFinder.byIdOptional(userId);

        if (!user.isPresent())
            throw new InvalidArgumentException(this.requestErrorMessage);

        if (!file.isPresent())
            throw new InvalidArgumentException(this.requestErrorMessage);

        if (!sessionManager.currentPolicy().canCreateUserPermission(file.get())) {
            logger.error(currentUser + " tried to create permissions for file " + file.get() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        boolean userAlreadyHasPermission = userPermissionFinder.findExistingPermissionForUser(fileId, user.get()).size() > 0;
        if(userAlreadyHasPermission)
            throw new InvalidArgumentException("Es existiert bereits eine Berechtigung");

        UserPermission permission = new UserPermission();
        permission.setFile(file.get());
        permission.setUser(user.get());

        CanReadWrite c = PermissionLevelConverter.ToReadWrite(permissionLevel);
        permission.setCanWrite(c.getCanWrite());
        permission.setCanRead(c.getCanRead());

        ebeanServer.save(permission);
        logger.info(currentUser + " created new permissions for user " + user.get() +  " on file " + file.get());
    }

    public void createGroupPermission(Long fileId, Long groupId, PermissionLevel permissionLevel) throws InvalidArgumentException, UnauthorizedException {
        User currentUser = this.sessionManager.currentUser();
        Optional<File> file = fileFinder.byIdOptional(fileId);
        Optional<Group> group = groupFinder.byIdOptional(groupId);

        if (!group.isPresent())
            throw new InvalidArgumentException(this.requestErrorMessage);

        if (!file.isPresent())
            throw new InvalidArgumentException(this.requestErrorMessage);

        if (!sessionManager.currentPolicy().canCreateGroupPermission(file.get(), group.get())) {
            logger.error(currentUser + " tried to create permissions for group " + group.get() + " for file " + file.get() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        boolean groupAlreadyHasPermission = groupPermissionFinder.findExistingPermissionForGroup(fileId, group.get()).size() > 0;
        if(groupAlreadyHasPermission)
            throw new InvalidArgumentException("Es existiert bereits eine Berechtigung");


        GroupPermission permission = new GroupPermission();
        permission.setFile(file.get());
        permission.setGroup(group.get());

        CanReadWrite c = PermissionLevelConverter.ToReadWrite(permissionLevel);
        permission.setCanWrite(c.getCanWrite());
        permission.setCanRead(c.getCanRead());

        ebeanServer.save(permission);
        logger.info(currentUser+ " created new permissions for group " + group.get() + " on file " + file.get());
    }

    public List<User> getAllOtherUsers(Long userId) {
        return userFinder.findAllButThis(userId);
    }

    public FileMeta getFileMeta(Long fileId) throws UnauthorizedException, InvalidArgumentException {
        return fileManager.getFileMeta(fileId);
    }
}
