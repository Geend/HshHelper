package models;

import io.ebean.Model;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

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


    @ManyToOne
    @JoinColumn(name = "written_by_id", referencedColumnName = "user_id")
    private User writtenBy;

    private DateTime writtenByDt;

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
        this.writtenBy = owner;
        this.writtenByDt = DateTime.now();
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
        if (data == null)
            return new byte[0];
        else
            return data;
    }

    public void setData(byte[] data) {
        if (data == null) {
            this.data = new byte[0];
        } else {
            this.data = data;
        }
        this.dataSize = (long) this.data.length;
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

    public User getWrittenBy() {
        return writtenBy;
    }

    public void setWrittenBy(User writtenBy) {
        this.writtenBy = writtenBy;
    }

    public DateTime getWrittenByDt() {
        return writtenByDt;
    }

    public void setWrittenByDt(DateTime writtenByDt) {
        this.writtenByDt = writtenByDt;
    }

    @Override
    public String toString() {
        return name + " (id: " + fileId + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        File file = (File) o;
        return Objects.equals(fileId, file.fileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId);
    }
}
