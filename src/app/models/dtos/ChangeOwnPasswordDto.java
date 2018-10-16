package models.dtos;

import play.data.validation.Constraints;
import play.data.validation.ValidationError;

@Constraints.Validate
public class ChangeOwnPasswordDto implements Constraints.Validatable<ValidationError>{
    private String password;
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
            return new ValidationError("password", "Passwords do not match");
        }
        return null;
    }
}
