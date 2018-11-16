package models.dtos;

import play.data.validation.Constraints;

public class GroupPermissionDto {
    @Constraints.Required
    private Long groupPermissionId;

    public Long getGroupPermissionId() {
        return groupPermissionId;
    }

    public GroupPermissionDto() {
    }

    public GroupPermissionDto(@Constraints.Required Long groupPermissionId) {
        this.groupPermissionId = groupPermissionId;
    }

    public void setGroupPermissionId(Long groupPermissionId) {
        this.groupPermissionId = groupPermissionId;
    }
}
