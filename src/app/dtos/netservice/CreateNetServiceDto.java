package dtos.netservice;

import play.data.validation.Constraints;

public class CreateNetServiceDto {
    @Constraints.Required
    private String name;

    @Constraints.Required
    private String url;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
