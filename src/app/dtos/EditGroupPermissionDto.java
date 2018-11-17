package dtos;

import models.PermissionLevel;
import play.data.validation.Constraints;

import java.util.List;

public class EditGroupPermissionDto {

    @Constraints.Required
    private Long groupPermissionId;

    @Constraints.Required
    private PermissionLevel permissionLevel;

    private List<PermissionLevel> possiblePermissions;

    public EditGroupPermissionDto() {
    }

    public EditGroupPermissionDto(@Constraints.Required Long groupPermissionId, @Constraints.Required PermissionLevel permissionLevel, List<PermissionLevel> possiblePermissions) {
        this.groupPermissionId = groupPermissionId;
        this.permissionLevel = permissionLevel;
        this.possiblePermissions = possiblePermissions;
    }

    public List<PermissionLevel> getPossiblePermissions() {
        return possiblePermissions;
    }

    public void setPossiblePermissions(List<PermissionLevel> possiblePermissions) {
        this.possiblePermissions = possiblePermissions;
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
}
