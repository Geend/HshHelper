package models;

import io.ebean.Model;

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


    public Long getNetServiceId() {
        return netServiceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
