package dtos.permissions;

import play.data.validation.Constraints;

import static policyenforcement.ConstraintValues.RETURN_URL_REGEX;

public class ShowEditGroupPermissionFormDto {
    private Long groupPermissionId;
    @Constraints.Pattern(RETURN_URL_REGEX)
    private String returnUrl;

    public ShowEditGroupPermissionFormDto() {
    }

    public Long getGroupPermissionId() {
        return groupPermissionId;
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
