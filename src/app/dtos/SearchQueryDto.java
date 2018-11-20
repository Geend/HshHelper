package dtos;

import play.data.validation.Constraints;

public class SearchQueryDto {

    @Constraints.Required
    //TODO: Add filename regex constraint
    private String query;


    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
