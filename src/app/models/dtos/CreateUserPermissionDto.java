package models.dtos;

import models.PermissionLevel;
import play.data.validation.Constraints;

public class CreateUserPermissionDto {

    @Constraints.Required
    private Long fileId;

    @Constraints.Required
    private Long userId;

    @Constraints.Required
    private PermissionLevel permissionLevel;


    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }
}
