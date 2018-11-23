package models.finders;

public class UserQuota {
    private long nameUsage;
    private long commentUsage;
    private long fileContentUsage;

    public long getNameUsage() {
        return nameUsage;
    }

    protected void setNameUsage(long nameUsage) {
        this.nameUsage = nameUsage;
    }

    public long getCommentUsage() {
        return commentUsage;
    }

    protected void setCommentUsage(long commentUsage) {
        this.commentUsage = commentUsage;
    }

    public long getFileContentUsage() {
        return fileContentUsage;
    }

    protected void setFileContentUsage(long fileContentUsage) {
        this.fileContentUsage = fileContentUsage;
    }

    public long getTotalUsage() {
        return nameUsage +commentUsage+fileContentUsage;
    }
}
