package dtos.user;

import play.data.validation.Constraints;
import play.data.validation.ValidationError;

import static policyenforcement.ConstraintValues.MAX_PASSWORD_LENGTH;
import static policyenforcement.ConstraintValues.MIN_PASSWORD_LENGTH;

@Constraints.Validate
public class ChangeOwnPasswordDto implements Constraints.Validatable<ValidationError>{

    @Constraints.Required
    @Constraints.MaxLength(MAX_PASSWORD_LENGTH)
    private String currentPassword;


    @Constraints.Required
    @Constraints.MaxLength(MAX_PASSWORD_LENGTH)
    @Constraints.MinLength(MIN_PASSWORD_LENGTH)
    private String newPassword;

    @Constraints.Required
    @Constraints.MaxLength(MAX_PASSWORD_LENGTH)
    @Constraints.MinLength(MIN_PASSWORD_LENGTH)
    private String newPasswordRepeat;


    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPasswordRepeat() {
        return newPasswordRepeat;
    }

    public void setNewPasswordRepeat(String newPasswordRepeat) {
        this.newPasswordRepeat = newPasswordRepeat;
    }

    @Override
    public ValidationError validate() {

        if(!newPassword.equals(newPasswordRepeat)){
            return new ValidationError("passwordRepeat", "Passwords do not match");
        }
        return null;
    }
}
