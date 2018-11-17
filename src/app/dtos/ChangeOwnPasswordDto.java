package dtos;

import play.data.validation.Constraints;
import play.data.validation.ValidationError;

import static policyenforcement.ConstraintValues.MAX_PASSWORD_LENGTH;

@Constraints.Validate
public class ChangeOwnPasswordDto implements Constraints.Validatable<ValidationError>{

    @Constraints.Required
    @Constraints.MaxLength(MAX_PASSWORD_LENGTH)
    private String password;

    @Constraints.Required
    @Constraints.MaxLength(MAX_PASSWORD_LENGTH)
    private String passwordRepeat;

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

    @Override
    public ValidationError validate() {

        if(!password.equals(passwordRepeat)){
            return new ValidationError("passwordRepeat", "Passwords do not match");
        }
        return null;
    }
}
