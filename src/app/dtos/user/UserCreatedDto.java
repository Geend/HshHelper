package dtos.user;

public class UserCreatedDto {


    private String username;

    private String plaintextPassword;


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPlaintextPassword() {
        return plaintextPassword;
    }

    public void setPlaintextPassword(String plaintextPassword) {
        this.plaintextPassword = plaintextPassword;
    }
}
