package dtos;

public class UploadFileMetaDto {
    private Long tempFileId;
    //TODO: Add filename regex constraint
    private String filename;
    private String comment;

    public UploadFileMetaDto() {

    }

    public UploadFileMetaDto(Long tempFileId, String filename, String comment) {
        this.tempFileId = tempFileId;
        this.filename = filename;
        this.comment = comment;
    }

    public Long getTempFileId() {
        return tempFileId;
    }

    public void setTempFileId(Long tempFileId) {
        this.tempFileId = tempFileId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
