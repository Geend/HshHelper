package models.dtos;

import models.PermissionLevel;
import play.data.validation.Constraints;

import java.util.List;

public class EditUserPermissionDto {

    @Constraints.Required
    private Long userPermissionId;

    @Constraints.Required
    private PermissionLevel permissionLevel;

    private List<PermissionLevel> possiblePermissions;

    public EditUserPermissionDto() {
    }

    public EditUserPermissionDto(@Constraints.Required Long userPermissionId, @Constraints.Required PermissionLevel permissionLevel, List<PermissionLevel> possiblePermissions) {
        this.userPermissionId = userPermissionId;
        this.permissionLevel = permissionLevel;
        this.possiblePermissions = possiblePermissions;
    }

    public List<PermissionLevel> getPossiblePermissions() {
        return possiblePermissions;
    }

    public void setPossiblePermissions(List<PermissionLevel> possiblePermissions) {
        this.possiblePermissions = possiblePermissions;
    }

    public Long getUserPermissionId() {
        return userPermissionId;
    }

    public void setUserPermissionId(Long groupPermissionId) {
        this.userPermissionId = groupPermissionId;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }
}

