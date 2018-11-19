package models;

import io.ebean.Model;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "groups")
public class Group extends Model {

    @Id
    private Long groupId;

    @Column(unique = true)
    private String name;
    @ManyToOne
    @JoinColumn(name = "owner_id", referencedColumnName = "user_id")
    private User owner;
    private boolean isAdminGroup;
    private boolean isAllGroup;

    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.REMOVE
    }, mappedBy = "groups")
    @OrderBy("username")
    private List<User> members = new ArrayList<>();

    @OneToMany(
            mappedBy = "group",
            cascade = CascadeType.ALL
    )
    private List<GroupPermission> groupPermissions = new ArrayList<>();

    public Group(Long id, String name, User owner, boolean isAdminGroup) {
        this.groupId = id;
        this.name = name;
        this.owner = owner;
        this.isAdminGroup = isAdminGroup;
    }

    public Group(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.isAdminGroup = false;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public boolean getIsAdminGroup() {
        return isAdminGroup;
    }

    public void setIsAdminGroup(boolean adminGroup) {
        isAdminGroup = adminGroup;
    }

    public boolean getIsAllGroup() {
        return isAllGroup;
    }

    public void setIsAllGroup(boolean allGroup) {
        isAllGroup = allGroup;
    }

    public List<User> getMembers() {
        return members;
    }

    public void setMembers(List<User> members) {
        this.members = members;
    }

    public List<GroupPermission> getGroupPermissions() {
        return groupPermissions;
    }

    public void setGroupPermissions(List<GroupPermission> groupPermissions) {
        this.groupPermissions = groupPermissions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return Objects.equals(groupId, group.groupId)&&
            Objects.equals(name, group.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, name);
    }
}