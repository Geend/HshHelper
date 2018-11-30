package extension.CredentialSerializer;

public class Credential {
    private String username;
    private String password;
    private String randomNoise;

    Credential(String username, String password, String randomNoise) {
        this.username = username;
        this.password = password;
        this.randomNoise = randomNoise;
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

    public String getRandomNoise() {
        return randomNoise;
    }

    public void setRandomNoise(String randomNoise) {
        this.randomNoise = randomNoise;
    }
}
