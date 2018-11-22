package dtos;

import play.data.validation.Constraints;

public class UserPermissionIdDto {
    @Constraints.Required
    private Long userPermissionId;

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

