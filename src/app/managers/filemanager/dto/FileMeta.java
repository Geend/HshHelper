package managers.filemanager.dto;

import org.joda.time.DateTime;

import java.util.List;

public class FileMeta {
    private Long fileId;
    private String filename;
    private String comment;
    private Long size;
    private Long totalSize;

    private Long ownerId;
    private String ownerName;

    private Long writtenById;
    private String writtenByName;
    private DateTime writtenByDt;

    private List<PermissionMeta> permissions;

    private boolean canReadFile;
    private boolean canReadPermissions;
    private boolean canWriteFile;
    private boolean canDeleteFile;

    public FileMeta(Long fileId, String filename, String comment, Long size, Long totalSize, Long ownerId, String ownerName, Long writtenById, String writtenByName, DateTime writtenByDt, List<PermissionMeta> permissions, boolean canReadFile, boolean canReadPermissions, boolean canWriteFile, boolean canDeleteFile) {
        this.fileId = fileId;
        this.filename = filename;
        this.comment = comment;
        this.size = size;
        this.totalSize = totalSize;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.writtenById = writtenById;
        this.writtenByName = writtenByName;
        this.writtenByDt = writtenByDt;
        this.permissions = permissions;
        this.canReadFile = canReadFile;
        this.canReadPermissions = canReadPermissions;
        this.canWriteFile = canWriteFile;
        this.canDeleteFile = canDeleteFile;
    }

    public Long getFileId() {
        return fileId;
    }

    public String getFilename() {
        return filename;
    }

    public String getComment() {
        return comment;
    }

    public Long getSize() {
        return size;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Long getWrittenById() {
        return writtenById;
    }

    public String getWrittenByName() {
        return writtenByName;
    }

    public DateTime getWrittenByDt() {
        return writtenByDt;
    }

    public List<PermissionMeta> getPermissions() {
        return permissions;
    }

    public boolean canReadFile() {
        return canReadFile;
    }

    public boolean canReadPermissions() {
        return canReadPermissions;
    }

    public boolean canWriteFile() {
        return canWriteFile;
    }

    public boolean canDeleteFile() {
        return canDeleteFile;
    }
}
