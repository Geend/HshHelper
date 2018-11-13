package models;

import io.ebean.Model;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "files")
public class File extends Model {
    @Id
    private Long fileId;
    private String name;
    private String comment;

    @Lob
    private byte[] data;

    @ManyToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "user_id")
    private User owner;


    @OneToMany(mappedBy = "file", fetch = FetchType.EAGER)
    private Set<UserPermission> userPermissions;


    @OneToMany(mappedBy = "file", fetch = FetchType.EAGER)
    private Set<GroupPermission> groupPermissions;


    public File() {
    }

    public File(String name, String comment, byte[] data, User owner) {
        this.name = name;
        this.comment = comment;
        this.data = data;
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
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

}