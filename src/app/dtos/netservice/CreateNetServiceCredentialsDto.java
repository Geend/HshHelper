package dtos.netservice;

import models.NetService;
import play.data.validation.Constraints;

public class CreateNetServiceCredentialsDto {

    @Constraints.Required
    private String username;

    @Constraints.Required
    private String password;

    @Constraints.Required
    private Long netServiceId;


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getNetServiceId() {
        return netServiceId;
    }

    public void setNetServiceId(Long netServiceId) {
        this.netServiceId = netServiceId;
    }
}
