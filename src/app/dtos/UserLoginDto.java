package dtos;

import play.data.validation.Constraints;

public class UserLoginDto {
    @Constraints.Required
    private String username;

    @Constraints.Required
    private String password;

    private String recaptcha;

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
