package dtos.file;

import models.PermissionLevel;

public class GroupPermissionDto {
    private Long groupId;
    private String groupName;
    private PermissionLevel permissionLevel;

    public GroupPermissionDto() {
    }

    public GroupPermissionDto(Long groupId, String groupName, PermissionLevel permissionLevel) {
        this.groupId = groupId;
        this.groupName = groupName;
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

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }
}
