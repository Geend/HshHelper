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
    @ManyToOne
    @JoinColumn(name = "owner", referencedColumnName = "id")
    public User owner;
    public boolean isAdminGroup;

    @ManyToMany(mappedBy = "groups")
    public Set<User> members = new HashSet<>();

    public static final GroupFinder find = new GroupFinder();

    public Group(Long id, String name, User owner, boolean isAdminGroup) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.isAdminGroup = isAdminGroup;
    }

    public Group(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.isAdminGroup = false;
    }

}