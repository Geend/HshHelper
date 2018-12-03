package models;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

@Entity
public class NetServiceParameter {

    public enum NetServiceParameterType{
        HIDDEN,
        USERNAME,
        PASSWORD
    }

    @Id
    private Long netServiceParameterId;

    private String name;
    private String defaultValue;


    @Enumerated(EnumType.STRING)
    private NetServiceParameterType parameterType;

    public NetServiceParameter() {
    }

    public NetServiceParameter(String name, NetServiceParameterType parameterType) {
        this.name = name;
        this.parameterType = parameterType;
        this.defaultValue = "";

    }

    public NetServiceParameter(String name, String defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public Long getNetServiceParameterId() {
        return netServiceParameterId;
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

    public NetServiceParameterType getParameterType() {
        return parameterType;
    }

    public void setParameterType(NetServiceParameterType parameterType) {
        this.parameterType = parameterType;
    }
}
