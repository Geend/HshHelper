package models;

import io.ebean.Model;
import play.data.validation.Constraints;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "netservices")
public class NetService extends Model {

    @Id
    private Long netServiceId;

    @Column(unique = true)
    private String name;

    private String url;

    private String usernameParameterName;

    private String passwordParameterName;




    public Long getNetServiceId() {
        return netServiceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsernameParameterName() {
        return usernameParameterName;
    }

    public void setUsernameParameterName(String usernameParameterName) {
        this.usernameParameterName = usernameParameterName;
    }

    public String getPasswordParameterName() {
        return passwordParameterName;
    }

    public void setPasswordParameterName(String passwordParameterName) {
        this.passwordParameterName = passwordParameterName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetService that = (NetService) o;
        return Objects.equals(netServiceId, that.netServiceId) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(netServiceId, name);
    }

    @Override
    public String toString() {
        return name + " (id: " + netServiceId + ")";
    }
}
