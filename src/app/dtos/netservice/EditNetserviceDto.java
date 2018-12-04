package dtos.netservice;

import play.data.validation.Constraints;

public class EditNetserviceDto {

    @Constraints.Required
    private Long netServiceId;

    @Constraints.Required
    private String name;

    @Constraints.Required
    private String url;

    public EditNetserviceDto() {

    }

    public EditNetserviceDto(Long netServiceId, String name, String url) {
        this.netServiceId = netServiceId;
        this.name = name;
        this.url = url;
    }

    public Long getNetServiceId() {
        return netServiceId;
    }

    public void setNetServiceId(Long netServiceId) {
        this.netServiceId = netServiceId;
    }

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
