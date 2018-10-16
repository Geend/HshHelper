package models;

import io.ebean.Model;
import models.finders.GroupFinder;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "groups")
public class Group extends Model {

    @Id
    public Long id;
    @Column(unique = true)
    public String name;
    @OneToOne
    @JoinColumn(name = "ownerId", referencedColumnName = "id")
    public User ownerId;
    public boolean isAdminGroup;

    @ManyToMany(mappedBy = "groups")
    public Set<User> members = new HashSet<>();

    public static final GroupFinder find = new GroupFinder();

    public Group(Long id, String name, User ownerId, boolean isAdminGroup) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.isAdminGroup = isAdminGroup;
    }

    public Group(String name, User ownerId) {
        this.name = name;
        this.ownerId = ownerId;
        this.isAdminGroup = false;
    }

    public static List<Group> findAll() {
        return find.all();
    }

    public static Optional<Group> getById(Long id) {
        return Optional.of(find.byId(id));
    }
}