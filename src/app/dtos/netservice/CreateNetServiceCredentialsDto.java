package dtos.netservice;

import play.data.validation.Constraints;

import javax.validation.Constraint;

public class CreateNetServiceCredentialsDto {

    @Constraints.Required
    @Constraints.MaxLength(40)
    private String username;

    @Constraints.Required
    @Constraints.MaxLength(40)
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
