package dtos.file;

import play.data.validation.Constraints;
import policyenforcement.ConstraintValues;

import static policyenforcement.ConstraintValues.MAX_FILENAME_LENGTH;

public class UploadFileDto {
    @Constraints.Required
    @Constraints.Pattern(ConstraintValues.FILENAME_REGEX)
    @Constraints.MaxLength(MAX_FILENAME_LENGTH)
    private String filename;

    private String comment;

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
