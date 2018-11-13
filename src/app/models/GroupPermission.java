package models;

import io.ebean.Model;

import javax.persistence.*;

@Entity
@Table(name = "group_permissions")
public class GroupPermission extends Model {
    @Id
    private Long groupPermissionId;

    @ManyToOne
    @JoinColumn(name = "file_id", referencedColumnName = "file_id")
    private File file;

    @ManyToOne
    @JoinColumn(name = "group_id", referencedColumnName = "group_id")
    private Group group;

    private Boolean canRead;
    private Boolean canWrite;


    public GroupPermission() {
    }

    public GroupPermission(File file, Group group, Boolean canRead, Boolean canWrite) {
        this.file = file;
        this.group = group;
        this.canRead = canRead;
        this.canWrite = canWrite;
    }

    public Long getGroupPermissionId() {
        return groupPermissionId;
    }

    public void setGroupPermissionId(Long groupPermissionId) {
        this.groupPermissionId = groupPermissionId;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public Boolean getCanRead() {
        return canRead;
    }

    public void setCanRead(Boolean canRead) {
        this.canRead = canRead;
    }

    public Boolean getCanWrite() {
        return canWrite;
    }

    public void setCanWrite(Boolean canWrite) {
        this.canWrite = canWrite;
    }
}
