package dtos;

import models.PermissionLevel;

public class UserPermissionDto {
    private Long userId;
    private String username;
    private PermissionLevel permissionLevel;

    public UserPermissionDto() {
    }

    public UserPermissionDto(Long userId, String username, PermissionLevel permissionLevel) {
        this.userId = userId;
        this.username = username;
        this.permissionLevel = permissionLevel;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }
}
