package models.finders;

public class UserQuota {
    private long nameUsage;
    private long commentUsage;
    private long fileContentUsage;
    private long tempFileContentUsage;

    public long getTempFileContentUsage() {
        return tempFileContentUsage;
    }

    protected void setTempFileContentUsage(long tempFileContentUsage) {
        this.tempFileContentUsage = tempFileContentUsage;
    }

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

    public void addFile(String name, String comment, byte[] data) {
        this.nameUsage += name.length();
        this.commentUsage += comment.length();
        this.fileContentUsage += data.length;
    }

    public long getTotalUsage() {
        return nameUsage +commentUsage+fileContentUsage + tempFileContentUsage;
    }
}
