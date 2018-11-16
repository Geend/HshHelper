package domainlogic.permissionmanager;

import domainlogic.InvalidArgumentException;
import domainlogic.UnauthorizedException;
import io.ebean.EbeanServer;
import models.*;
import models.File;
import models.GroupPermission;
import models.UserPermission;
import models.dtos.EditGroupPermissionDto;
import models.dtos.EditUserPermissionDto;
import models.dtos.PermissionEntryDto;
import models.finders.*;
import models.finders.FileFinder;
import models.finders.GroupPermissionFinder;
import models.finders.UserPermissionFinder;
import policy.Specification;

import javax.inject.Inject;
import java.util.*;

public class PermissionManager {
    private UserPermissionFinder userPermissionFinder;
    private GroupPermissionFinder groupPermissionFinder;
    private FileFinder fileFinder;
    private GroupFinder groupFinder;
    private UserFinder userFinder;
private EbeanServer ebeanServer;
    private Specification specification;

    @Inject
    public PermissionManager(UserPermissionFinder userPermissionFinder, GroupPermissionFinder groupPermissionFinder, FileFinder fileFinder, GroupFinder groupFinder, UserFinder userFinder, EbeanServer ebeanServer, Specification specification) {
        this.userPermissionFinder = userPermissionFinder;
        this.groupPermissionFinder = groupPermissionFinder;
        this.fileFinder = fileFinder;
        this.groupFinder = groupFinder;
        this.userFinder = userFinder;
        this.ebeanServer = ebeanServer;
        this.specification = specification;
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

    public Set<User> getAllOtherUsers(Long userId) {
        return userFinder.query().where().notIn("userId", userId).findSet();
    }

    public List<File> getUserFiles(Long userId) {
        return fileFinder.getFilesByOwner(userId);
    }

    public void editGroupPermission(Long userId, Long groupPermissionId, PermissionLevel newLevel) throws InvalidArgumentException {
        // TODO: authorization
        GroupPermission groupPermission = this.groupPermissionFinder.byId(groupPermissionId);
        CanReadWrite c = this.PermissionLevelToCanReadWrite(newLevel);
        groupPermission.setCanWrite(c.canWrite);
        groupPermission.setCanRead(c.canRead);
        this.ebeanServer.save(groupPermission);
    }

    public void editUserPermission(Long userId, Long userPermissionId, PermissionLevel newLevel) throws InvalidArgumentException {
        // TODO: authorization
        UserPermission userPermission = this.userPermissionFinder.byId(userPermissionId);
        CanReadWrite c = this.PermissionLevelToCanReadWrite(newLevel);
        userPermission.setCanWrite(c.canWrite);
        userPermission.setCanRead(c.canRead);
        this.ebeanServer.save(userPermission);
    }

    public EditGroupPermissionDto getGroupPermissionForEdit(Long userId, Long groupPermissionId) throws InvalidDataException {
        // TODO: authorization
        GroupPermission permission = this.groupPermissionFinder.byId(groupPermissionId);
        PermissionLevel permissionLevel = this.fromReadWrite(permission.getCanRead(), permission.getCanWrite());
        List<PermissionLevel> possiblePermissions = Arrays.asList(PermissionLevel.values());
        return new EditGroupPermissionDto(permission.getGroupPermissionId(), permissionLevel, possiblePermissions);
    }

    public EditUserPermissionDto getUserPermissionForEdit(Long userId, Long userPermissionId) throws InvalidDataException {
        // TODO: authorization
        UserPermission permission = this.userPermissionFinder.byId(userPermissionId);
        PermissionLevel permissionLevel = this.fromReadWrite(permission.getCanRead(), permission.getCanWrite());
        List<PermissionLevel> possiblePermissions = Arrays.asList(PermissionLevel.values());
        return new EditUserPermissionDto(permission.getUserPermissionId(), permissionLevel, possiblePermissions);
    }

    public void deleteGroupPermission(Long userId, Long groupPermissionId) {
        // TODO: authorization
        GroupPermission permission = this.groupPermissionFinder.byId(groupPermissionId);
        this.ebeanServer.delete(permission);
    }

    public void deleteUserPermission(Long userId, Long userPermissionId) {
        // TODO: authorization
        UserPermission permission = this.userPermissionFinder.byId(userPermissionId);
        this.ebeanServer.delete(permission);
    }

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
            throw new InvalidArgumentException("Dieser User existiert nicht.");

        if (!file.isPresent())
            throw new InvalidArgumentException("Diese Datei existiert nicht.");

        if (!specification.CanCreateUserPermission(file.get(), currentUser))
            throw new UnauthorizedException();

        boolean userAlreadyHasPermission = userPermissionFinder.findForFileId(fileId).stream().anyMatch(x -> x.getUser().equals(user.get()));
        if(userAlreadyHasPermission)
            throw new InvalidArgumentException("Der Benutzer hat bereits eine Berechtigung auf diese Datei.");

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
            throw new InvalidArgumentException("Dieser Gruppe existiert nicht.");

        if (!file.isPresent())
            throw new InvalidArgumentException("Diese Datei existiert nicht.");

        if (!specification.CanCreateGroupPermission(file.get(), currentUser, group.get()))
            throw new UnauthorizedException();


        boolean groupAlreadyHasPermission = groupPermissionFinder.findForFileId(fileId).stream().anyMatch(x -> x.getGroup().equals(group.get()));
        if(groupAlreadyHasPermission)
            throw new InvalidArgumentException("Der Gruppe hat bereits eine Berechtigung auf diese Datei.");


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

    class CanReadWrite
    {
        public boolean canRead;
        public boolean canWrite;
    }
}
