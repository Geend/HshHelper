package models;

import io.ebean.Model;

import javax.persistence.*;

@Entity
@Table(name = "user_permissions")
public class UserPermission extends Model {
    @Id
    private Long userPermissionId;

    @ManyToOne
    @JoinColumn(name = "fk_file_id", referencedColumnName = "file_id")
    private File file;

    @ManyToOne
    @JoinColumn(name = "fk_user_id", referencedColumnName = "user_id")
    private User user;

    private Boolean canRead;
    private Boolean canWrite;


    public UserPermission() {
    }

    public UserPermission(File file, User user, Boolean canRead, Boolean canWrite) {
        this.file = file;
        this.user = user;
        this.canRead = canRead;
        this.canWrite = canWrite;
    }

    public Long getUserPermissionId() {
        return userPermissionId;
    }

    public void setUserPermissionId(Long userPermissionId) {
        this.userPermissionId = userPermissionId;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
