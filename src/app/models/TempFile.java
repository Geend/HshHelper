package models;

import io.ebean.Model;
import org.joda.time.DateTime;

import javax.persistence.*;


@Entity
@Table(name = "temp_files")
public class TempFile extends Model {
    @Id
    private Long fileId;

    @Lob
    private byte[] data;

    @ManyToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "user_id")
    private User owner;

    private DateTime created;

    public TempFile() {

    }

    public TempFile(User owner, byte[] data) {
        this.data = data;
        this.owner = owner;
        this.created = DateTime.now();
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
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

    public DateTime getCreated() {
        return created;
    }

    public void setCreated(DateTime created) {
        this.created = created;
    }
}
