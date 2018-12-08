package dtos.file;

import play.data.validation.Constraints;

public class EditFileContentDto {
    @Constraints.Required
    public Long fileId;

    public EditFileContentDto() {
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }
}
