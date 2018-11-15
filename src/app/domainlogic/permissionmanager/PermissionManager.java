package domainlogic.permissionmanager;

import models.File;
import models.GroupPermission;
import models.UserPermission;
import models.dtos.PermissionEntryDto;
import models.finders.FileFinder;
import models.finders.GroupFinder;
import models.finders.GroupPermissionFinder;
import models.finders.UserPermissionFinder;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class PermissionManager {
    private UserPermissionFinder userPermissionFinder;
    private GroupPermissionFinder groupPermissionFinder;
    private FileFinder fileFinder;

    @Inject
    public PermissionManager(UserPermissionFinder userPermissionFinder, GroupPermissionFinder groupPermissionFinder, FileFinder fileFinder) {
        this.userPermissionFinder = userPermissionFinder;
        this.groupPermissionFinder = groupPermissionFinder;
        this.fileFinder = fileFinder;
    }

    private String getPermissionString(boolean canRead, boolean canWrite) {
        if(canWrite) {
            return "write";
        }
        else if(canRead) {
            return "read";
        }
        else {
            return "none";
        }
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
                result.add(new PermissionEntryDto(index++, "Group", groupPermission.getGroup().getName(), permissionString, true, fileName));
            }
            List<UserPermission> userPermissions = this.userPermissionFinder.findForFileId(ownedFile.getFileId());
            for (UserPermission userPermission : userPermissions) {
                String permissionString = this.getPermissionString(userPermission.getCanRead(), userPermission.getCanWrite());
                result.add(new PermissionEntryDto(index++, "User", userPermission.getUser().getUsername(), permissionString, false, fileName));
            }
        }
        return result;
    }
}
