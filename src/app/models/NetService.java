package models;

import io.ebean.Model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "netservices")
public class NetService extends Model {

    @Id
    private Long netServiceId;

    @Column(unique = true)
    private String name;

    private String url;

    @OneToMany(
            cascade = CascadeType.ALL
    )
    private List<NetServiceParameter> parameters = new ArrayList<>();

    @OneToMany(mappedBy = "netService", cascade = CascadeType.ALL)
    private List<NetServiceCredential> credentials = new ArrayList<>();


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

    public List<NetServiceCredential> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<NetServiceCredential> credentials) {
        this.credentials = credentials;
    }

    public List<NetServiceParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<NetServiceParameter> parameters) {
        this.parameters = parameters;
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
