package dtos.permissions;

import play.data.validation.Constraints;

import static policyenforcement.ConstraintValues.RETURN_URL_REGEX;

public class GroupPermissionIdDto {
    @Constraints.Required
    private Long groupPermissionId;

    @Constraints.Pattern(RETURN_URL_REGEX)
    private String returnUrl;

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

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
}
