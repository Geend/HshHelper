package managers.filemanager.dto;

public class PermissionMeta {
    public static enum EType { USER, GROUP }

    private Long id;
    private EType type;
    private Long refId;
    private String refName;
    private boolean canRead;
    private boolean canWrite;

    private boolean canEditPermission;
    private boolean canDeletePermission;

    public PermissionMeta(Long id, EType type, Long refId, String refName, boolean canRead, boolean canWrite, boolean canEditPermission, boolean canDeletePermission) {
        this.id = id;
        this.type = type;
        this.refId = refId;
        this.refName = refName;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.canEditPermission = canEditPermission;
        this.canDeletePermission = canDeletePermission;
    }

    public Long getId() {
        return id;
    }

    public EType getType() {
        return type;
    }

    public Long getRefId() {
        return refId;
    }

    public String getRefName() {
        return refName;
    }

    public boolean canRead() {
        return canRead;
    }

    public boolean canWrite() {
        return canWrite;
    }

    public boolean canEditPermission() {
        return canEditPermission;
    }

    public boolean canDeletePermission() {
        return canDeletePermission;
    }
}
