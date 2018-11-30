package dtos;

import play.data.validation.Constraints;
import policyenforcement.ConstraintValues;

public class UserLoginDto {
    @Constraints.Required
    private String username;

    @Constraints.Required
    private String password;

    private String recaptcha;

    @Constraints.Pattern(ConstraintValues.SECOND_FACTOR_NUMBER)
    private String twofactorpin;

    public String getTwofactorpin() {
        return twofactorpin;
    }

    public void setTwofactorpin(String twofactorpin) {
        this.twofactorpin = twofactorpin;
    }

    public String getRecaptcha() {
        return recaptcha;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
