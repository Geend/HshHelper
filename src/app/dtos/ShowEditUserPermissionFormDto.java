package dtos;

import play.data.validation.Constraints;

import static policyenforcement.ConstraintValues.RETURN_URL_REGEX;

public class ShowEditUserPermissionFormDto {
    private Long userPermissionId;
    @Constraints.Pattern(RETURN_URL_REGEX)
    private String returnUrl;

    public ShowEditUserPermissionFormDto() {
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
