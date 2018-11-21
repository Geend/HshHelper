package dtos;

import java.util.List;

public class UploadFileDto {
    //TODO: Add filename regex constraint
    private String filename;
    private String comment;
    private List<UserPermissionDto> userPermissions;
    private List<GroupPermissionDto> groupPermissions;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<UserPermissionDto> getUserPermissions() {
        return userPermissions;
    }

    public void setUserPermissions(List<UserPermissionDto> userPermissions) {
        this.userPermissions = userPermissions;
    }

    public List<GroupPermissionDto> getGroupPermissions() {
        return groupPermissions;
    }

    public void setGroupPermissions(List<GroupPermissionDto> groupPermissions) {
        this.groupPermissions = groupPermissions;
    }
}
