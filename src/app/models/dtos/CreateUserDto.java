package models.dtos;

import models.User;
import play.data.validation.Constraints;
import validation.HsHConstraints;

import static policy.ConstraintValues.MAX_USERNAME_LENGTH;

public class CreateUserDto {

    @Constraints.Required
    @Constraints.MaxLength(MAX_USERNAME_LENGTH)
    @HsHConstraints.Unique(model = User.class, columns = "username", message = "Dieser Username existiert bereits")
    private String username;

    @Constraints.Required
    @Constraints.Email
    private String email;

    @Constraints.Required
    private Integer quotaLimit;


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getQuotaLimit() {
        return quotaLimit;
    }

    public void setQuotaLimit(Integer quotaLimit) {
        this.quotaLimit = quotaLimit;
    }

}
