package models;

import io.ebean.Model;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "files")
public class File extends Model {
    @Id
    private Long fileId;
    private String name;
    @Lob
    private String comment;

    @Lob
    private byte[] data;
    private Long dataSize;

    @ManyToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "user_id")
    private User owner;

    @OneToMany(
            cascade = CascadeType.ALL,
            mappedBy = "file",
            fetch = FetchType.EAGER
    )
    private List<UserPermission> userPermissions;

    @OneToMany(
            cascade = CascadeType.ALL,
            mappedBy = "file",
            fetch = FetchType.EAGER
    )
    private List<GroupPermission> groupPermissions;

    public File() {
    }

    public File(String name, String comment, byte[] data, User owner) {
        this.name = name;
        this.comment = comment;
        this.data = data;
        this.dataSize = (long) data.length;
        this.owner = owner;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        this.dataSize = (long)data.length;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Long getTotalSize() {
        return this.dataSize + this.name.length() + this.comment.length();
    }

    public List<UserPermission> getUserPermissions() {
        return userPermissions;
    }

    public List<GroupPermission> getGroupPermissions() {
        return groupPermissions;
    }

    public Long getDataSize() {
        return dataSize;
    }
}
