package domainlogic.permissionmanager;

import domainlogic.InvalidArgumentException;
import domainlogic.UnauthorizedException;
import io.ebean.EbeanServer;
import models.*;
import models.File;
import models.GroupPermission;
import models.UserPermission;
import models.dtos.PermissionEntryDto;
import models.finders.*;
import models.finders.FileFinder;
import models.finders.GroupPermissionFinder;
import models.finders.UserPermissionFinder;
import policy.Specification;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    public List<PermissionEntryDto> GetAllGrantedPermissions(Long userId) {
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
                        groupPermission.getGroup().getGroupId()));
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
                        userPermission.getUser().getUserId()));
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

        boolean userAlreadyHaPermission = userPermissionFinder.findForFileId(fileId).stream().anyMatch(x -> x.getUser().equals(user.get()));
        if(userAlreadyHaPermission)
            throw new InvalidArgumentException("Der Benutzer hat bereits eine Berechtigung auf diese Datei.");



        UserPermission permission = new UserPermission();
        permission.setFile(file.get());
        permission.setUser(user.get());

        switch (permissionLevel) {
            case READ:
                permission.setCanRead(true);
                permission.setCanWrite(false);
                break;
            case WRITE:
                permission.setCanRead(false);
                permission.setCanWrite(true);
                break;
            default:
                throw new InvalidArgumentException("Permission Level ungültig");
        }


        ebeanServer.save(permission);

    }
}
