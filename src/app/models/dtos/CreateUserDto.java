package models.dtos;

import play.data.validation.Constraints;

import static policy.ConstraintValues.MAX_USERNAME_LENGTH;

public class CreateUserDto {


    @Constraints.Required
    @Constraints.MaxLength(MAX_USERNAME_LENGTH)
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
