package dtos.login;

import play.data.validation.Constraints;
import play.data.validation.ValidationError;

import java.util.UUID;

import static policyenforcement.ConstraintValues.MAX_PASSWORD_LENGTH;

public class ResetPasswordDto implements Constraints.Validatable<ValidationError> {
    @Constraints.Required
    @Constraints.MaxLength(MAX_PASSWORD_LENGTH)
    private String newPassword;

    @Constraints.Required
    @Constraints.MaxLength(MAX_PASSWORD_LENGTH)
    private String newPasswordRepeated;

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

    @Override
    public ValidationError validate() {

        if(!newPassword.equals(newPasswordRepeated)){
            return new ValidationError("newPasswordRepeated", "Passwords do not match");
        }

        return null;
    }
}
