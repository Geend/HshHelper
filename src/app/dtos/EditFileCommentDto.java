package dtos;

public class EditFileCommentDto {
    private Long fileId;
    private String comment;

    public EditFileCommentDto() {
    }

    public EditFileCommentDto(Long fileId, String comment) {
        this.fileId = fileId;
        this.comment = comment;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
