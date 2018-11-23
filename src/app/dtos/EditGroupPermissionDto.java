package dtos;

import models.PermissionLevel;
import play.data.validation.Constraints;

public class EditGroupPermissionDto {

    @Constraints.Required
    private Long groupPermissionId;

    @Constraints.Required
    private PermissionLevel permissionLevel;

    private Long groupId;

    private String groupName;

    private String returnUrl;

    private Long fileId;

    private String filename;


    public EditGroupPermissionDto() {
    }

    public EditGroupPermissionDto(@Constraints.Required Long groupPermissionId, @Constraints.Required PermissionLevel permissionLevel, Long groupId, String groupName, Long fileId, String filename) {
        this.groupPermissionId = groupPermissionId;
        this.permissionLevel = permissionLevel;
        this.groupId = groupId;
        this.groupName = groupName;
        this.fileId = fileId;
        this.filename = filename;
    }

    public Long getGroupPermissionId() {
        return groupPermissionId;
    }

    public void setGroupPermissionId(Long groupPermissionId) {
        this.groupPermissionId = groupPermissionId;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
