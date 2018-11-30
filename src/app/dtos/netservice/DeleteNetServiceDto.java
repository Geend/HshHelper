package dtos.netservice;

import play.data.validation.Constraints;

public class DeleteNetServiceDto {
    @Constraints.Required
    private Long netServiceId;

    public DeleteNetServiceDto() {
    }

    public DeleteNetServiceDto(Long netServiceId) {
        this.netServiceId = netServiceId;
    }


    public Long getNetServiceId() {
        return netServiceId;
    }

    public void setNetServiceId(Long netServiceId) {
        this.netServiceId = netServiceId;
    }
}
