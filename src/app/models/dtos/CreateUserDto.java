package models.dtos;

import io.ebean.Finder;
import models.User;
import models.finders.UserFinder;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;
import validation.ValidatableWithFinder;
import validation.ValidateWithUserFinder;

import static policy.ConstraintValues.MAX_USERNAME_LENGTH;

@ValidateWithUserFinder
public class CreateUserDto implements ValidatableWithFinder<ValidationError, User> {


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

    @Override
    public ValidationError validate(Finder<Long, User> finder) {
        UserFinder userFinder = (UserFinder)finder;
        if(userFinder.byName(this.username).isPresent()) {
            return new ValidationError("username", "Benutzername muss uniqe sein");
        }
        return null;
    }
}
