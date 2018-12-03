package dtos.netservice;

import play.data.validation.Constraints;
import policyenforcement.ConstraintValues;

public class CreateNetServiceDto {
    @Constraints.Required
    private String name;

    @Constraints.Required
    private String url;

    @Constraints.Required
    private String usernameParameterName;

    @Constraints.Required
    private String passwordParameterName;


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

    public String getUsernameParameterName() {
        return usernameParameterName;
    }

    public void setUsernameParameterName(String usernameParameterName) {
        this.usernameParameterName = usernameParameterName;
    }

    public String getPasswordParameterName() {
        return passwordParameterName;
    }

    public void setPasswordParameterName(String passwordParameterName) {
        this.passwordParameterName = passwordParameterName;
    }
}
