package dtos.file;

import play.data.validation.Constraints;

public class DeleteFileDto {
    @Constraints.Required
    private Long fileId;

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }
}
