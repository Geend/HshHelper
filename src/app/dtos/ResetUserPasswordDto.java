package dtos;

import play.data.validation.Constraints;
import policyenforcement.ConstraintValues;

public class ResetUserPasswordDto {

    @Constraints.Required
    @Constraints.MaxLength(ConstraintValues.MAX_USERNAME_LENGTH)
    @Constraints.Pattern(ConstraintValues.USERNAME_REGEX)
    private String username;

    private String recaptcha;

    public String getRecaptcha() {
        return this.recaptcha;
    }

    public void setRecaptcha(String recaptcha) {
        this.recaptcha = recaptcha;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
