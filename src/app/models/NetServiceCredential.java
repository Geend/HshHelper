package models;


import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class NetServiceCredential {
    @Id
    private Long netServiceCredentialId;

    private byte[] initializationVectorUsername;
    private byte[] usernameCipherText;
    private byte[] initializationVectorPassword;
    private byte[] passwordCipherText;

    @ManyToOne
    private NetService netService;

    @ManyToOne
    @JoinColumn(name = "fk_user_id", referencedColumnName = "user_id")
    private User user;

    public void setNetServiceCredentialId(Long netServiceCredentialId) {
        this.netServiceCredentialId = netServiceCredentialId;
    }

    public byte[] getInitializationVectorUsername() {
        return initializationVectorUsername;
    }

    public void setInitializationVectorUsername(byte[] initializationVectorUsername) {
        this.initializationVectorUsername = initializationVectorUsername;
    }

    public byte[] getUsernameCipherText() {
        return usernameCipherText;
    }

    public void setUsernameCipherText(byte[] usernameCipherText) {
        this.usernameCipherText = usernameCipherText;
    }

    public byte[] getInitializationVectorPassword() {
        return initializationVectorPassword;
    }

    public void setInitializationVectorPassword(byte[] initializationVectorPassword) {
        this.initializationVectorPassword = initializationVectorPassword;
    }

    public byte[] getPasswordCipherText() {
        return passwordCipherText;
    }

    public void setPasswordCipherText(byte[] passwordCipherText) {
        this.passwordCipherText = passwordCipherText;
    }

    public NetService getNetService() {
        return netService;
    }

    public void setNetService(NetService netService) {
        this.netService = netService;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getNetServiceCredentialId() {
        return netServiceCredentialId;
    }
}
