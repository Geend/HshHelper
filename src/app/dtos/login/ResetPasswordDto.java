package dtos.login;

import play.data.validation.Constraints;
import play.data.validation.ValidationError;
import policyenforcement.ConstraintValues;

import javax.validation.Constraint;
import java.util.UUID;

import static policyenforcement.ConstraintValues.MAX_PASSWORD_LENGTH;
import static policyenforcement.ConstraintValues.MIN_PASSWORD_LENGTH;

public class ResetPasswordDto implements Constraints.Validatable<ValidationError> {
    @Constraints.Required
    @Constraints.MaxLength(MAX_PASSWORD_LENGTH)
    @Constraints.MinLength(MIN_PASSWORD_LENGTH)
    private String newPassword;

    @Constraints.Required
    @Constraints.MaxLength(MAX_PASSWORD_LENGTH)
    @Constraints.MinLength(MIN_PASSWORD_LENGTH)
    private String newPasswordRepeated;

    @Constraints.Pattern(ConstraintValues.SECOND_FACTOR_NUMBER)
    private String twoFactorPin;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPasswordRepeated() {
        return newPasswordRepeated;
    }

    public void setNewPasswordRepeated(String newPasswordRepeated) {
        this.newPasswordRepeated = newPasswordRepeated;
    }



    public String getTwoFactorPin() {
        return twoFactorPin;
    }

    public void setTwoFactorPin(String twoFactorPin) {
        this.twoFactorPin = twoFactorPin;
    }

    @Override
    public ValidationError validate() {

        if(!newPassword.equals(newPasswordRepeated)){
            return new ValidationError("newPasswordRepeated", "Passwords do not match");
        }

        return null;
    }
}
