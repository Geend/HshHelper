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

    public GroupPermission getGroupPermission(Long groupPermissionId) throws InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<GroupPermission> permission = this.groupPermissionFinder.byIdOptional(groupPermissionId);

        if (!permission.isPresent())
            throw new InvalidArgumentException();

        if (!policy.CanViewGroupPermission(user, permission.get()))
            throw new UnauthorizedException();

        return permission.get();
    }

    public UserPermission getUserPermission(Long userPermissionId) throws UnauthorizedException, InvalidArgumentException {
        User user = this.sessionManager.currentUser();
        Optional<UserPermission> permission = this.userPermissionFinder.byIdOptional(userPermissionId);

        if (!permission.isPresent())
            throw new InvalidArgumentException();

        if (!policy.CanViewUserPermission(user, permission.get()))
            throw new UnauthorizedException();

        return permission.get();
    }

    //
    //  group permissions
    //

    public void editGroupPermission(Long groupPermissionId, PermissionLevel newLevel) throws InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<GroupPermission> permission = this.groupPermissionFinder.byIdOptional(groupPermissionId);

        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!policy.CanEditGroupPermission(user, permission.get()))
            throw new UnauthorizedException();

        CanReadWrite c = this.PermissionLevelToCanReadWrite(newLevel);
        permission.get().setCanWrite(c.canWrite);
        permission.get().setCanRead(c.canRead);

        this.ebeanServer.save(permission.get());
    }

    public EditGroupPermissionDto getGroupPermissionForEdit(Long groupPermissionId) throws InvalidDataException, InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<GroupPermission> permission = this.groupPermissionFinder.byIdOptional(groupPermissionId);

        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!policy.CanEditGroupPermission(user, permission.get()))
            throw new UnauthorizedException();

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

        if (!policy.CanDeleteGroupPermission(user, permission.get()))
            throw new UnauthorizedException();

        this.ebeanServer.delete(permission.get());
    }

    //
    // user permissions
    //

    public EditUserPermissionDto getUserPermissionForEdit(Long userPermissionId) throws InvalidDataException, InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<UserPermission> permission = this.userPermissionFinder.byIdOptional(userPermissionId);

        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!policy.CanEditUserPermission(user, permission.get()))
            throw new UnauthorizedException();

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

        if (!policy.CanDeleteUserPermission(user, permission.get()))
            throw new UnauthorizedException();

        this.ebeanServer.delete(permission.get());
    }

    public void editUserPermission(Long userPermissionId, PermissionLevel newLevel) throws InvalidArgumentException, UnauthorizedException {
        User user = this.sessionManager.currentUser();
        Optional<UserPermission> permission = this.userPermissionFinder.byIdOptional(userPermissionId);

        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!policy.CanEditUserPermission(user, permission.get()))
            throw new UnauthorizedException();

        CanReadWrite c = this.PermissionLevelToCanReadWrite(newLevel);
        permission.get().setCanWrite(c.canWrite);
        permission.get().setCanRead(c.canRead);
        this.ebeanServer.save(permission.get());
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

        if (!policy.CanCreateUserPermission(file.get(), currentUser))
            throw new UnauthorizedException();

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
    }

    public void createGroupPermission(Long fileId, Long groupId, PermissionLevel permissionLevel) throws InvalidArgumentException, UnauthorizedException {
        User currentUser = this.sessionManager.currentUser();
        Optional<File> file = fileFinder.byIdOptional(fileId);
        Optional<Group> group = groupFinder.byIdOptional(groupId);

        if (!group.isPresent())
            throw new InvalidArgumentException(this.requestErrorMessage);

        if (!file.isPresent())
            throw new InvalidArgumentException(this.requestErrorMessage);

        if (!policy.CanCreateGroupPermission(file.get(), currentUser, group.get()))
            throw new UnauthorizedException();


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
            case READWRITE:
                result.canRead = true;
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
