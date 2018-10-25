package models.dtos;

import io.ebean.Finder;
import models.User;
import models.checks.*;
import models.finders.UserFinder;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;
import validation.ValidatableWithFinder;
import validation.ValidateWithUserFinder;

import static policy.ConstraintValues.MAX_PASSWORD_LENGTH;
import static policy.ConstraintValues.MAX_USERNAME_LENGTH;

@ValidateWithUserFinder(groups = {CreateUserCheck.class})
@Constraints.Validate(groups = {ChangePasswordCheck.class})
public class PartialUserForm implements ValidatableWithFinder<ValidationError, User>, Constraints.Validatable<ValidationError> {

    @Constraints.Required(groups = {
            CreateUserCheck.class, LoginCheck.class, ResetUserPasswordCheck.class,
            ChangePasswortAfterResetUsernameCheck.class
    })
    @Constraints.MaxLength(value = MAX_USERNAME_LENGTH, groups = {
            CreateUserCheck.class, LoginCheck.class, ResetUserPasswordCheck.class,
            ChangePasswortAfterResetUsernameCheck.class
    })
    // UserLoginDTO, CreateUserDTO,
    // ChangePWAfterResetDTO, ResetUserPasswordDTO
    private String username;

    @Constraints.Required(groups = {LoginCheck.class, ChangePasswordCheck.class})
    @Constraints.MaxLength(value = MAX_PASSWORD_LENGTH,
            groups = {LoginCheck.class, ChangePasswordCheck.class})
    // UserLoginDTO, ChangePWAfterResetDTO, ChangeOwnPasswordDTO
    private String password;

    @Constraints.Required(groups = {ChangePasswordCheck.class})
    // ChangePWAfterResetDTO, ChangeOwnPasswordDTO
    private String currentPassword;

    @Constraints.Required(groups = {ChangePasswordCheck.class})
    @Constraints.MaxLength(value = MAX_PASSWORD_LENGTH,
            groups = {ChangePasswordCheck.class})
    // ChangePWAfterResetDTO, ChangeOwnPasswordDTO
    private String passwordRepeat;

    // UserLoginDTO
    private String recaptcha;

    @Constraints.Required(groups = {CreateUserCheck.class})
    @Constraints.Email(groups = {CreateUserCheck.class})
    // CreateUserDTO
    private String email;

    @Constraints.Required(groups = {CreateUserCheck.class})
    // CreateUserDTO
    private Integer quotaLimit;

    @Override
    public ValidationError validate() {
        if(!password.equals(passwordRepeat)){
            return new ValidationError("passwordRepeat", "Passwords do not match");
        }
        return null;
    }

    @Override
    public ValidationError validate(Finder<Long, User> finder) {
        UserFinder userFinder = (UserFinder)finder;
        if(userFinder.byName(this.username).isPresent()) {
            return new ValidationError("username", "Benutzername muss uniqe sein");
        }
        return null;
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

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getPasswordRepeat() {
        return passwordRepeat;
    }

    public void setPasswordRepeat(String passwordRepeat) {
        this.passwordRepeat = passwordRepeat;
    }

    public String getRecaptcha() {
        return recaptcha;
    }

    public void setRecaptcha(String recaptcha) {
        this.recaptcha = recaptcha;
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
