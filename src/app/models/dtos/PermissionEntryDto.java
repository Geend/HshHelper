package models.dtos;

public class PermissionEntryDto {

    private int index;
    private String type;
    private String text;
    private String permission;
    private Boolean isGroupPermission;

    public PermissionEntryDto() {
    }

    public PermissionEntryDto(int index, String type, String text, String permission, Boolean isGroupPermission) {
        this.index = index;
        this.type = type;
        this.text = text;
        this.permission = permission;
        this.isGroupPermission = isGroupPermission;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean getIsGroupPermission() {
        return isGroupPermission;
    }

    public void setIsGroupPermission(Boolean groupPermission) {
        isGroupPermission = groupPermission;
    }
}
