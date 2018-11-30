package dtos.permissions;

import play.data.validation.Constraints;

import static policyenforcement.ConstraintValues.RETURN_URL_REGEX;

public class UserPermissionIdDto {
    @Constraints.Required
    private Long userPermissionId;

    @Constraints.Pattern(RETURN_URL_REGEX)
    private String returnUrl;

    public UserPermissionIdDto() {
    }

    public UserPermissionIdDto(@Constraints.Required Long userPermissionId) {
        this.userPermissionId = userPermissionId;
    }

    public Long getUserPermissionId() {
        return userPermissionId;
    }

    public void setUserPermissionId(Long userPermissionId) {
        this.userPermissionId = userPermissionId;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
}

