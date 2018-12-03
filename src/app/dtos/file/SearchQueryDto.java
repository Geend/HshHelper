package dtos.file;

import play.data.validation.Constraints;
import policyenforcement.ConstraintValues;

public class SearchQueryDto {

    @Constraints.Required
    @Constraints.Pattern(ConstraintValues.FILENAME_REGEX)
    private String query;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
