package managers.filemanager;

import managers.filemanager.dto.FileMeta;
import managers.filemanager.dto.PermissionMeta;
import models.File;
import models.GroupPermission;
import models.UserPermission;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class FileMetaFactory {
    private SessionManager sessionManager;

    @Inject
    public FileMetaFactory(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public List<FileMeta> fromFiles(List<File> files) {
        List<FileMeta> meta = new ArrayList<>();

        for(File file : files) {
            meta.add(fromFile(file));
        }

        return meta;
    }

    public FileMeta fromFile(File file) {
        ArrayList<PermissionMeta> permissions = null;
        if(sessionManager.currentPolicy().canViewFilePermissions(file)) {
            permissions = new ArrayList<>();

            for(UserPermission userPermission : file.getUserPermissions()) {
                PermissionMeta permission = new PermissionMeta(
                        userPermission.getUserPermissionId(),
                        PermissionMeta.EType.USER,
                        userPermission.getUser().getUserId(),
                        userPermission.getUser().getUsername(),
                        userPermission.getCanRead(),
                        userPermission.getCanWrite(),
                        sessionManager.currentPolicy().canEditUserPermission(userPermission),
                        sessionManager.currentPolicy().canDeleteUserPermission(userPermission)
                );
                permissions.add(permission);
            }

            for (GroupPermission groupPermission : file.getGroupPermissions()) {
                PermissionMeta permission = new PermissionMeta(
                        groupPermission.getGroupPermissionId(),
                        PermissionMeta.EType.GROUP,
                        groupPermission.getGroup().getGroupId(),
                        groupPermission.getGroup().getName(),
                        groupPermission.getCanRead(),
                        groupPermission.getCanWrite(),
                        sessionManager.currentPolicy().canEditGroupPermission(groupPermission),
                        sessionManager.currentPolicy().canDeleteGroupPermission(groupPermission)
                );
                permissions.add(permission);
            }
        }

        return new FileMeta(
                file.getFileId(),
                file.getName(),
                file.getComment(),
                file.getDataSize(),
                file.getTotalSize(),
                file.getOwner().getUserId(),
                file.getOwner().getUsername(),
                file.getWrittenBy().getUserId(),
                file.getWrittenBy().getUsername(),
                file.getWrittenByDt(),
                permissions,
                sessionManager.currentPolicy().canReadFile(file),
                sessionManager.currentPolicy().canViewFilePermissions(file),
                sessionManager.currentPolicy().canWriteFile(file),
                sessionManager.currentPolicy().canDeleteFile(file)
        );
    }
}
