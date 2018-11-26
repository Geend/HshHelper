package dtos;

import models.PermissionLevel;
import play.data.validation.Constraints;

import javax.validation.Constraint;
import java.util.List;

import static policyenforcement.ConstraintValues.RETURN_URL_REGEX;

public class EditUserPermissionDto {

    @Constraints.Required
    private Long userPermissionId;

    @Constraints.Required
    private PermissionLevel permissionLevel;

    private List<PermissionLevel> possiblePermissions;

    private String username;

    @Constraints.Pattern(RETURN_URL_REGEX)
    private String returnUrl;

    private Long fileId;

    private String filename;

    public EditUserPermissionDto() {
    }

    public EditUserPermissionDto(@Constraints.Required Long userPermissionId, @Constraints.Required PermissionLevel permissionLevel, List<PermissionLevel> possiblePermissions, String username, Long fileId, String filename) {
        this.userPermissionId = userPermissionId;
        this.permissionLevel = permissionLevel;
        this.possiblePermissions = possiblePermissions;
        this.username = username;
        this.fileId = fileId;
        this.filename = filename;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}

