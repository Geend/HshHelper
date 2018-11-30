package dtos.permissions;

import models.PermissionLevel;
import play.data.validation.Constraints;

public class CreateGroupPermissionDto {

    @Constraints.Required
    private Long fileId;

    @Constraints.Required
    private Long groupId;

    @Constraints.Required
    private PermissionLevel permissionLevel;


    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }
}
