package models;


import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class NetServiceCredential {


    @Id
    private Long netServiceCredentialId;



    byte[] credential;

    @ManyToOne
    private NetService netService;


    @ManyToOne
    @JoinColumn(name = "fk_user_id", referencedColumnName = "user_id")
    private User user;


    public byte[] getCredential() {
        return credential;
    }

    public void setCredential(byte[] credential) {
        this.credential = credential;
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
