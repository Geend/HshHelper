package managers.permissionmanager;

import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import io.ebean.EbeanServer;
import models.*;
import models.File;
import models.GroupPermission;
import models.UserPermission;
import dtos.EditGroupPermissionDto;
import dtos.EditUserPermissionDto;
import dtos.PermissionEntryDto;
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
            SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.userPermissionFinder = userPermissionFinder;
        this.groupPermissionFinder = groupPermissionFinder;
        this.fileFinder = fileFinder;
        this.groupFinder = groupFinder;
        this.userFinder = userFinder;
        this.ebeanServer = ebeanServer;
        this.policy = policy;
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

        PermissionLevel permissionLevel = this.fromReadWrite(permission.get().getCanRead(), permission.get().getCanWrite());
        List<PermissionLevel> possiblePermissions = Arrays.asList(PermissionLevel.values());
        return new EditGroupPermissionDto(permission.get().getGroupPermissionId(), permissionLevel, possiblePermissions);
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
        return new EditUserPermissionDto(permission.get().getUserPermissionId(), permissionLevel, possiblePermissions);
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
    //  all permissions
    //

    public List<PermissionEntryDto> getAllGrantedPermissions() {
        User user = this.sessionManager.currentUser();
        Integer index = 0;
        ArrayList<PermissionEntryDto> result = new ArrayList<>();
        List<File> ownedFiles = this.fileFinder.getFilesByOwner(user.getUserId());
        for (File ownedFile : ownedFiles) {
            List<GroupPermission> groupPermissionsForFile = this.groupPermissionFinder.findForFileId(ownedFile.getFileId());
            String fileName = ownedFile.getName();
            for (GroupPermission groupPermission : groupPermissionsForFile) {
                String permissionString = this.getPermissionString(groupPermission.getCanRead(), groupPermission.getCanWrite());
                result.add(new PermissionEntryDto(
                        index++,
                        "Group",
                        groupPermission.getGroup().getName(),
                        permissionString,
                        true,
                        fileName,
                        groupPermission.getGroupPermissionId()));
            }
            List<UserPermission> userPermissions = this.userPermissionFinder.findForFileId(ownedFile.getFileId());
            for (UserPermission userPermission : userPermissions) {
                String permissionString = this.getPermissionString(userPermission.getCanRead(), userPermission.getCanWrite());
                result.add(new PermissionEntryDto(
                        index++,
                        "User",
                        userPermission.getUser().getUsername(),
                        permissionString,
                        false,
                        fileName,
                        userPermission.getUserPermissionId()));
            }
        }
        return result;
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

    public List<File> getUserFiles(Long userId) {
        return fileFinder.getFilesByOwner(userId);
    }

    private PermissionLevel fromReadWrite(boolean canRead, boolean canWrite) throws InvalidDataException {
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
            default:
                throw new InvalidArgumentException("Permission Level ungültig");
        }
        return result;
    }

    private String getPermissionString(boolean canRead, boolean canWrite) {
        if (canWrite) {
            return "write";
        } else if (canRead) {
            return "read";
        } else {
            return "none";
        }
    }

    class CanReadWrite
    {
        boolean canRead;
        boolean canWrite;
    }
}
