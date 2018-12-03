package dtos.netservice;

import models.NetServiceParameter;
import play.data.validation.Constraints;

public class AddNetServiceParameterDto {

    @Constraints.Required
    private Long netServiceId;

    @Constraints.Required
    private String name;

    @Constraints.Required
    private NetServiceParameter.NetServiceParameterType parameterType;

    private String defaultValue;


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

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public NetServiceParameter.NetServiceParameterType getParameterType() {
        return parameterType;
    }

    public void setParameterType(NetServiceParameter.NetServiceParameterType parameterType) {
        this.parameterType = parameterType;
    }
}
