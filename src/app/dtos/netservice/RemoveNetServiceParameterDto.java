package dtos.netservice;

import models.NetServiceParameter;
import play.data.validation.Constraints;

import javax.validation.Constraint;

public class RemoveNetServiceParameterDto {

    @Constraints.Required
    private Long netServiceId;

    @Constraints.Required
    private Long netServiceParameterId;

    public Long getNetServiceId() {
        return netServiceId;
    }

    public void setNetServiceId(Long netServiceId) {
        this.netServiceId = netServiceId;
    }

    public Long getNetServiceParameterId() {
        return netServiceParameterId;
    }

    public void setNetServiceParameterId(Long netServiceParameterId) {
        this.netServiceParameterId = netServiceParameterId;
    }
}
