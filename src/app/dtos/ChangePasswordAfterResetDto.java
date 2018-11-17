package dtos;

import play.data.validation.Constraints;
import play.data.validation.ValidationError;
import policyenforcement.ConstraintValues;

import java.util.Objects;

import static policyenforcement.ConstraintValues.MAX_PASSWORD_LENGTH;
import static policyenforcement.ConstraintValues.MAX_USERNAME_LENGTH;

@Constraints.Validate
public class ChangePasswordAfterResetDto implements Constraints.Validatable<ValidationError>{

    @Constraints.Required
    @Constraints.MaxLength(MAX_USERNAME_LENGTH)
    @Constraints.Pattern(ConstraintValues.USERNAME_REGEX)
    private String username;

    @Constraints.Required
    @Constraints.MaxLength(MAX_PASSWORD_LENGTH)
    private String currentPassword;

    @Constraints.Required
    @Constraints.MaxLength(MAX_PASSWORD_LENGTH)
    private String password;

    @Constraints.Required
    @Constraints.MaxLength(MAX_PASSWORD_LENGTH)
    private String passwordRepeat;

    private String recaptcha;

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

    public String getPasswordRepeat() {
        return passwordRepeat;
    }

    public void setPasswordRepeat(String passwordRepeat) {
        this.passwordRepeat = passwordRepeat;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getRecaptcha() {
        return this.recaptcha;
    }

    public void setRecaptcha(String recaptcha) {
        this.recaptcha = recaptcha;
    }

    @Override
    public ValidationError validate() {

        if(!Objects.equals(password, passwordRepeat)){
            return new ValidationError("passwordRepeat", "Passwords do not match");
        }
        return null;
    }
}

