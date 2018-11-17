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

import javax.inject.Inject;
import java.util.*;

public class PermissionManager {
    private UserPermissionFinder userPermissionFinder;
    private GroupPermissionFinder groupPermissionFinder;
    private FileFinder fileFinder;
    private GroupFinder groupFinder;
    private UserFinder userFinder;
    private EbeanServer ebeanServer;
    private Policy policy;
    private String requestErrorMessage;

    @Inject
    public PermissionManager(UserPermissionFinder userPermissionFinder, GroupPermissionFinder groupPermissionFinder, FileFinder fileFinder, GroupFinder groupFinder, UserFinder userFinder, EbeanServer ebeanServer, Policy policy) {
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

    public void editGroupPermission(Long userId, Long groupPermissionId, PermissionLevel newLevel) throws InvalidArgumentException, UnauthorizedException {
        Optional<User> user = this.userFinder.byIdOptional(userId);
        Optional<GroupPermission> permission = this.groupPermissionFinder.byIdOptional(groupPermissionId);

        if (!user.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);
        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!policy.CanEditGroupPermission(user.get(), permission.get()))
            throw new UnauthorizedException();

        CanReadWrite c = this.PermissionLevelToCanReadWrite(newLevel);
        permission.get().setCanWrite(c.canWrite);
        permission.get().setCanRead(c.canRead);

        this.ebeanServer.save(permission.get());
    }

    public EditGroupPermissionDto getGroupPermissionForEdit(Long userId, Long groupPermissionId) throws InvalidDataException, InvalidArgumentException, UnauthorizedException {
        Optional<User> user = this.userFinder.byIdOptional(userId);
        Optional<GroupPermission> permission = this.groupPermissionFinder.byIdOptional(groupPermissionId);

        if (!user.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);
        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!policy.CanEditGroupPermission(user.get(), permission.get()))
            throw new UnauthorizedException();

        PermissionLevel permissionLevel = this.fromReadWrite(permission.get().getCanRead(), permission.get().getCanWrite());
        List<PermissionLevel> possiblePermissions = Arrays.asList(PermissionLevel.values());
        return new EditGroupPermissionDto(permission.get().getGroupPermissionId(), permissionLevel, possiblePermissions);
    }

    public void deleteGroupPermission(Long userId, Long groupPermissionId) throws InvalidArgumentException, UnauthorizedException {
        Optional<User> user = this.userFinder.byIdOptional(userId);
        Optional<GroupPermission> permission = this.groupPermissionFinder.byIdOptional(groupPermissionId);

        if (!user.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);
        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!policy.CanDeleteGroupPermission(user.get(), permission.get()))
            throw new UnauthorizedException();

        this.ebeanServer.delete(permission.get());
    }

    //
    // user permissions
    //

    public EditUserPermissionDto getUserPermissionForEdit(Long userId, Long userPermissionId) throws InvalidDataException, InvalidArgumentException, UnauthorizedException {
        Optional<User> user = this.userFinder.byIdOptional(userId);
        Optional<UserPermission> permission = this.userPermissionFinder.byIdOptional(userPermissionId);

        if (!user.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);
        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!policy.CanEditUserPermission(user.get(), permission.get()))
            throw new UnauthorizedException();

        PermissionLevel permissionLevel = this.fromReadWrite(permission.get().getCanRead(), permission.get().getCanWrite());
        List<PermissionLevel> possiblePermissions = Arrays.asList(PermissionLevel.values());
        return new EditUserPermissionDto(permission.get().getUserPermissionId(), permissionLevel, possiblePermissions);
    }

    public void deleteUserPermission(Long userId, Long userPermissionId) throws InvalidArgumentException, UnauthorizedException {
        Optional<User> user = this.userFinder.byIdOptional(userId);
        Optional<UserPermission> permission = this.userPermissionFinder.byIdOptional(userPermissionId);

        if (!user.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);
        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!policy.CanDeleteUserPermission(user.get(), permission.get()))
            throw new UnauthorizedException();

        this.ebeanServer.delete(permission.get());
    }

    public void editUserPermission(Long userId, Long userPermissionId, PermissionLevel newLevel) throws InvalidArgumentException, UnauthorizedException {
        Optional<User> user = this.userFinder.byIdOptional(userId);
        Optional<UserPermission> permission = this.userPermissionFinder.byIdOptional(userPermissionId);

        if (!user.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);
        if (!permission.isPresent())
            throw new InvalidArgumentException(requestErrorMessage);

        if (!policy.CanEditUserPermission(user.get(), permission.get()))
            throw new UnauthorizedException();

        CanReadWrite c = this.PermissionLevelToCanReadWrite(newLevel);
        permission.get().setCanWrite(c.canWrite);
        permission.get().setCanRead(c.canRead);
        this.ebeanServer.save(permission.get());
    }

    //
    //  all permissions and create
    //

    public List<PermissionEntryDto> getAllGrantedPermissions(Long userId) {
        Integer index = 0;
        ArrayList<PermissionEntryDto> result = new ArrayList<>();
        List<File> ownedFiles = this.fileFinder.getFilesByOwner(userId);
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

    public void createUserPermission(User currentUser, Long fileId, Long userId, PermissionLevel permissionLevel) throws InvalidArgumentException, UnauthorizedException {

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
            throw new InvalidArgumentException(this.requestErrorMessage);

        UserPermission permission = new UserPermission();
        permission.setFile(file.get());
        permission.setUser(user.get());

        CanReadWrite c = this.PermissionLevelToCanReadWrite(permissionLevel);
        permission.setCanWrite(c.canWrite);
        permission.setCanRead(c.canRead);

        ebeanServer.save(permission);
    }

    public void createGroupPermission(User currentUser, Long fileId, Long groupId, PermissionLevel permissionLevel) throws InvalidArgumentException, UnauthorizedException {
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
            throw new InvalidArgumentException(this.requestErrorMessage);


        GroupPermission permission = new GroupPermission();
        permission.setFile(file.get());
        permission.setGroup(group.get());

        CanReadWrite c = this.PermissionLevelToCanReadWrite(permissionLevel);
        permission.setCanWrite(c.canWrite);
        permission.setCanRead(c.canRead);

        ebeanServer.save(permission);

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

    public Set<User> getAllOtherUsers(Long userId) {
        return userFinder.query().where().notIn("userId", userId).findSet();
    }

    public List<File> getUserFiles(Long userId) {
        return fileFinder.getFilesByOwner(userId);
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
        public boolean canRead;
        public boolean canWrite;
    }
}