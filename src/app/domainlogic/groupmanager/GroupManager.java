package domainlogic.groupmanager;

import domainlogic.UnauthorizedException;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import models.Group;
import models.User;
import models.finders.GroupFinder;
import models.finders.UserFinder;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupManager {

    private final GroupFinder groupFinder;
    private final UserFinder userFinder;
    private final EbeanServer ebeanServer;

    @Inject
    public GroupManager(GroupFinder groupFinder, UserFinder userFinder, EbeanServer ebeanServer) {
        this.groupFinder = groupFinder;
        this.userFinder = userFinder;
        this.ebeanServer = ebeanServer;
    }

    public void createGroup(Long userId, String groupName) throws GroupNameAlreadyExistsException {
        Optional<User> currentUserOptional = userFinder.byIdOptional(userId);
        if (!currentUserOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new IllegalArgumentException("Dieser User existiert nicht.");
        }
        User currentUser = currentUserOptional.get();


        try(Transaction tx = ebeanServer.beginTransaction(TxIsolation.REPEATABLE_READ)) {
            Optional<Group> txGroup = groupFinder.byName(groupName);
            if(txGroup.isPresent()) {
                throw new GroupNameAlreadyExistsException("Gruppe existiert bereits.");
            }

            Group group = new Group(groupName, currentUser);
            group.getMembers().add(currentUser);
            ebeanServer.save(group);

            tx.commit();
        }
    }

    public Set<Group> getOwnGroups(Long userId) {
        Optional<User> currentUserOptional = userFinder.byIdOptional(userId);
        if (!currentUserOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new IllegalArgumentException("Dieser User existiert nicht.");
        }
        User currentUser = currentUserOptional.get();

        return currentUser.getGroups();
    }

    public Group getGroup(Long userId, Long groupId) {
        Optional<Group> groupOptional = groupFinder.byIdOptional(groupId);
        Optional<User> currentUserOptional = userFinder.byIdOptional(userId);
        if (!groupOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new IllegalArgumentException("Diese Gruppe existiert nicht.");
        }
        if (!currentUserOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new IllegalArgumentException("Dieser User existiert nicht.");
        }

        Group group = groupOptional.get();
        User currentUser = currentUserOptional.get();

        // TODO: Does this need a policy check if the user can "get" this group?

        return group;
    }

    public Set<User> getGroupMembers(Long userId, Long groupId) throws UnauthorizedException {
        Optional<Group> groupOptional = groupFinder.byIdOptional(groupId);
        Optional<User> currentUserOptional = userFinder.byIdOptional(userId);
        if (!groupOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new IllegalArgumentException("Diese Gruppe existiert nicht.");
        }
        if (!currentUserOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new IllegalArgumentException("Dieser User existiert nicht.");
        }

        Group group = groupOptional.get();
        User currentUser = currentUserOptional.get();

        if (!policy.Specification.instance.CanViewGroupDetails(currentUser, group)) {
            throw new UnauthorizedException("Du bist nicht authorisiert, die Mitglieder dieser Gruppe zu sehen.");
        }

        return group.getMembers();
    }

    public Set<User> getUsersWhichAreNotInThisGroup(Long userId, Long groupId) throws UnauthorizedException {
        Optional<Group> groupOptional = groupFinder.byIdOptional(groupId);
        Optional<User> currentUserOptional = userFinder.byIdOptional(userId);
        if (!groupOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new IllegalArgumentException("Diese Gruppe existiert nicht.");
        }
        if (!currentUserOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new IllegalArgumentException("Dieser User existiert nicht.");
        }

        Group group = groupOptional.get();
        User currentUser = currentUserOptional.get();

        if (!policy.Specification.instance.CanViewGroupDetails(currentUser, group)) {
            throw new UnauthorizedException("Du bist nicht authorisiert, die Mitglieder dieser Gruppe zu sehen.");
        }


        return userFinder.query().where().notIn("userId",
                group.getMembers().stream().map(user -> user.getUserId()).collect(Collectors.toSet())
        ).findSet();
    }

    public void removeGroupMember(Long userId, Long userToRemove, Long groupId) throws UnauthorizedException {
        Optional<User> currentUserOptional = userFinder.byIdOptional(userId);
        Optional<User> tobeRemovedUserOptional = userFinder.byIdOptional(userToRemove);
        Optional<Group> groupOptional = groupFinder.byIdOptional(groupId);

        if (!groupOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new IllegalArgumentException("Diese Gruppe existiert nicht.");
        }
        if (!currentUserOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new IllegalArgumentException("Dieser User existiert nicht.");
        }
        if (!tobeRemovedUserOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new IllegalArgumentException("Dieser User kann nicht geloescht werden, weil er nicht existiert.");
        }

        Group g = groupOptional.get();
        User currentUser = currentUserOptional.get();
        User toBeRemovedUser = tobeRemovedUserOptional.get();

        if(!policy.Specification.instance.CanRemoveGroupMember(currentUser, g, toBeRemovedUser)) {
            throw new UnauthorizedException("Du bist nicht authorisiert, einen Member aus dieser Gruppe zu loeschen.");
        }

        g.getMembers().remove(toBeRemovedUser);
        ebeanServer.save(g);
    }

    public void addGroupMember(Long userId, Long userToAdd, Long groupId) throws UnauthorizedException {
        Optional<User> currentUserOptional = userFinder.byIdOptional(userId);
        Optional<User> tobeAddedUserOptional = userFinder.byIdOptional(userToAdd);
        Optional<Group> groupOptional = groupFinder.byIdOptional(groupId);

        if (!groupOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new IllegalArgumentException("Diese Gruppe existiert nicht.");
        }
        if (!currentUserOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new IllegalArgumentException("Dieser User existiert nicht.");
        }
        if (!tobeAddedUserOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new IllegalArgumentException("Dieser User kann nicht hinzugefuegt werden, weil er nicht existiert.");
        }

        Group g = groupOptional.get();
        User currentUser = currentUserOptional.get();
        User toBeAddedUser = tobeAddedUserOptional.get();

        if (!policy.Specification.instance.CanAddGroupMember(currentUser, g, toBeAddedUser)) {
            throw new UnauthorizedException("Du bist nicht authorisiert, einen Member zu dieser Gruppe hinzu zu fuegen.");
        }

        g.getMembers().add(toBeAddedUser);
        ebeanServer.save(g);
    }

    public void deleteGroup(Long currentUser, Long groupId) throws UnauthorizedException, IllegalArgumentException {
        Optional<Group> groupOptional = groupFinder.byIdOptional(groupId);
        Optional<User> currentUserOptional = userFinder.byIdOptional(currentUser);
        if (!groupOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new IllegalArgumentException("Diese Gruppe existiert nicht.");
        }
        if (!currentUserOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new IllegalArgumentException("Dieser User existiert nicht.");
        }

        Group g = groupOptional.get();
        User u = currentUserOptional.get();
        if (!policy.Specification.instance.CanDeleteGroup(u, g)) {
            throw new UnauthorizedException("Du bist nicht authorisiert, eine Gruppe zu loeschen.");
        }

        ebeanServer.delete(g);
    }
}
