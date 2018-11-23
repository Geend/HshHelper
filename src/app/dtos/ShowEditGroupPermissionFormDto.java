package dtos;

public class ShowEditGroupPermissionFormDto {
    private Long groupPermissionId;
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
