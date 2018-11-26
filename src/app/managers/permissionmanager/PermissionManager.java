package managers.permissionmanager;

import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import io.ebean.EbeanServer;
import managers.filemanager.FileManager;
import models.*;
import models.File;
import models.GroupPermission;
import models.UserPermission;
import dtos.EditGroupPermissionDto;
import dtos.EditUserPermissionDto;
import models.finders.*;
import models.finders.FileFinder;
import models.finders.GroupPermissionFinder;
import models.finders.UserPermissionFinder;
import play.Logger;
import policyenforcement.Policy;
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
    private final Policy policy;
    private final String requestErrorMessage;
    private final FileManager fileManager;

    private static final Logger.ALogger logger = Logger.of(PermissionManager.class);

    @Inject
    public PermissionManager(
            UserPermissionFinder userPermissionFinder,
            GroupPermissionFinder groupPermissionFinder,
            FileFinder fileFinder,
            GroupFinder groupFinder,
            UserFinder userFinder,
            EbeanServer ebeanServer,
            Policy policy,
            SessionManager sessionManager, FileManager fileManager) {
        this.sessionManager = sessionManager;
        this.userPermissionFinder = userPermissionFinder;
        this.groupPermissionFinder = groupPermissionFinder;
        this.fileFinder = fileFinder;
        this.groupFinder = groupFinder;
        this.userFinder = userFinder;
        this.ebeanServer = ebeanServer;
        this.policy = policy;
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

        if (!policy.CanEditGroupPermission(user, permission.get())) {
            logger.error(user.getUsername() + " tried to change the permissions for group " + permission.get().getGroup().getName() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        CanReadWrite c = this.PermissionLevelToCanReadWrite(newLevel);
        permission.get().setCanWrite(c.canWrite);
        permission.get().setCanRead(c.canRead);

        this.ebeanServer.save(permission.get());
        logger.info(user.getUsername() + " changed permissions for group " + permission.get().getGroup().getName() + "to canWrite: " + c.canWrite + " and canRead: " + c.canRead);
    }

    public EditGroupPermissionDto getGroupPermissionForEdit(Long groupPermissionId) throws InvalidDataException, InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<GroupPermission> permission = this.groupPermissionFinder.byIdOptional(groupPermissionId);

        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!policy.CanEditGroupPermission(user, permission.get())) {
            logger.error(user.getUsername() + " tried to edit the permissions for group " + permission.get().getGroup().getName() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        GroupPermission gp = permission.get();

        return new EditGroupPermissionDto(
            gp.getGroupPermissionId(),
            fromReadWrite(gp.getCanRead(), gp.getCanWrite()),
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

        if (!policy.CanDeleteGroupPermission(user, permission.get())) {
            logger.error(user.getUsername() + " tried to delete permissions for group " + permission.get().getGroup().getName() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        this.ebeanServer.delete(permission.get());
        logger.info(user.getUsername() + " deleted permissions for group" + permission.get().getGroup().getName());
    }

    //
    // user permissions
    //

    public EditUserPermissionDto getUserPermissionForEdit(Long userPermissionId) throws InvalidDataException, InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<UserPermission> permission = this.userPermissionFinder.byIdOptional(userPermissionId);

        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!policy.CanEditUserPermission(user, permission.get())) {
            logger.error(user.getUsername() + " tried to edit permissions for user " + permission.get().getUser().getUsername() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        PermissionLevel permissionLevel = this.fromReadWrite(permission.get().getCanRead(), permission.get().getCanWrite());
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

        if (!policy.CanDeleteUserPermission(user, permission.get())) {
            logger.error(user.getUsername() + " tried to delete permissions for user " + permission.get().getUser().getUsername() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        this.ebeanServer.delete(permission.get());
        logger.info(user.getUsername() + " deleted permissions for user" + permission.get().getUser().getUsername());
    }

    public void editUserPermission(Long userPermissionId, PermissionLevel newLevel) throws InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<UserPermission> permission = this.userPermissionFinder.byIdOptional(userPermissionId);

        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!policy.CanEditUserPermission(user, permission.get())) {
            logger.error(user.getUsername() + " tried to change the permissions for user " + permission.get().getUser().getUsername() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        CanReadWrite c = this.PermissionLevelToCanReadWrite(newLevel);
        permission.get().setCanWrite(c.canWrite);
        permission.get().setCanRead(c.canRead);
        this.ebeanServer.save(permission.get());
        logger.info(user.getUsername() + " changed permissions for user " + permission.get().getUser().getUsername() + "to canWrite: " + c.canWrite + " and canRead: " + c.canRead);
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

        if (!policy.CanCreateUserPermission(file.get(), currentUser)) {
            logger.error(currentUser.getUsername() + " tried to create permissions for file " + file.get().getName() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        boolean userAlreadyHasPermission = userPermissionFinder.findForFileId(fileId).stream().anyMatch(x -> x.getUser().equals(user.get()));
        if(userAlreadyHasPermission)
            throw new InvalidArgumentException("Es existiert bereits eine Berechtigung");

        UserPermission permission = new UserPermission();
        permission.setFile(file.get());
        permission.setUser(user.get());

        CanReadWrite c = this.PermissionLevelToCanReadWrite(permissionLevel);
        permission.setCanWrite(c.canWrite);
        permission.setCanRead(c.canRead);

        ebeanServer.save(permission);
        logger.info(currentUser.getUsername() + " created new permissions for user " + user.get().getUsername() +  " on file " + file.get().getName());
    }

    public void createGroupPermission(Long fileId, Long groupId, PermissionLevel permissionLevel) throws InvalidArgumentException, UnauthorizedException {
        User currentUser = this.sessionManager.currentUser();
        Optional<File> file = fileFinder.byIdOptional(fileId);
        Optional<Group> group = groupFinder.byIdOptional(groupId);

        if (!group.isPresent())
            throw new InvalidArgumentException(this.requestErrorMessage);

        if (!file.isPresent())
            throw new InvalidArgumentException(this.requestErrorMessage);

        if (!policy.CanCreateGroupPermission(file.get(), currentUser, group.get())) {
            logger.error(currentUser.getUsername() + " tried to create permissions for group " + group.get().getName() + " for file " + file.get().getName() + " but he is not authorized");
            throw new UnauthorizedException();
        }

        boolean groupAlreadyHasPermission = groupPermissionFinder.findForFileId(fileId).stream().anyMatch(x -> x.getGroup().equals(group.get()));
        if(groupAlreadyHasPermission)
            throw new InvalidArgumentException("Es existiert bereits eine Berechtigung");


        GroupPermission permission = new GroupPermission();
        permission.setFile(file.get());
        permission.setGroup(group.get());

        CanReadWrite c = this.PermissionLevelToCanReadWrite(permissionLevel);
        permission.setCanWrite(c.canWrite);
        permission.setCanRead(c.canRead);

        ebeanServer.save(permission);
        logger.info(currentUser.getUsername() + " created new permissions for group " + group.get().getName() +  " on file " + file.get().getName());
    }

    public List<User> getAllOtherUsers(Long userId) {
        return userFinder.findAllButThis(userId);
    }

    public File getFile(Long fileId) throws UnauthorizedException, InvalidArgumentException {
        return fileManager.getFile(fileId);
    }

    private PermissionLevel fromReadWrite(boolean canRead, boolean canWrite) throws InvalidDataException {
        if(canRead && canWrite) {
            return PermissionLevel.READWRITE;
        }
        if(canWrite) {
            return PermissionLevel.WRITE;
        }
        else if(canRead) {
            return PermissionLevel.READ;
        }
        throw new InvalidDataException("no permission level exists for no read and no write access");
    }

    private CanReadWrite PermissionLevelToCanReadWrite(PermissionLevel l) throws InvalidArgumentException {
        CanReadWrite result = new CanReadWrite();
        switch (l) {
            case READ:
                result.canRead =true;
                result.canWrite = false;
                break;
            case WRITE:
                result.canRead = false;
                result.canWrite = true;
                break;
            case READWRITE:
                result.canRead = true;
                result.canWrite = true;
                break;
            default:
                throw new InvalidArgumentException("Permission Level ungültig");
        }
        return result;
    }

    class CanReadWrite
    {
        boolean canRead;
        boolean canWrite;
    }
}
