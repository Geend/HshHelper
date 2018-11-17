package dtos;

import play.data.validation.Constraints;

public class GroupPermissionIdDto {
    @Constraints.Required
    private Long groupPermissionId;

    public Long getGroupPermissionId() {
        return groupPermissionId;
    }

    public GroupPermissionIdDto() {
    }

    public GroupPermissionIdDto(@Constraints.Required Long groupPermissionId) {
        this.groupPermissionId = groupPermissionId;
    }

    public void setGroupPermissionId(Long groupPermissionId) {
        this.groupPermissionId = groupPermissionId;
    }
}
