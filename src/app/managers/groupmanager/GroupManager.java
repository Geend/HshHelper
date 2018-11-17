package managers.groupmanager;

import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import models.Group;
import models.User;
import models.finders.GroupFinder;
import models.finders.UserFinder;
import policyenforcement.Policy;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupManager {

    private final GroupFinder groupFinder;
    private final UserFinder userFinder;
    private final EbeanServer ebeanServer;
    private final SessionManager sessionManager;
    private final Policy policy;

    @Inject
    public GroupManager(GroupFinder groupFinder, UserFinder userFinder, EbeanServer ebeanServer, SessionManager sessionManager, Policy policy) {
        this.groupFinder = groupFinder;
        this.userFinder = userFinder;
        this.ebeanServer = ebeanServer;
        this.sessionManager = sessionManager;
        this.policy = policy;
    }

    public void createGroup(String groupName) throws GroupNameAlreadyExistsException, InvalidArgumentException {
        User user = sessionManager.currentUser();

        try(Transaction tx = ebeanServer.beginTransaction(TxIsolation.REPEATABLE_READ)) {
            Optional<Group> txGroup = groupFinder.byName(groupName);
            if(txGroup.isPresent()) {
                throw new GroupNameAlreadyExistsException("Gruppe existiert bereits.");
            }

            Group group = new Group(groupName, user);
            group.getMembers().add(user);
            ebeanServer.save(group);

            tx.commit();
        }
    }

    public Set<Group> getAllGroups() throws InvalidArgumentException, UnauthorizedException {
        if(!Policy.instance.CanSeeAllGroups(sessionManager.currentUser()))
            throw new UnauthorizedException();

        return new HashSet<Group>(groupFinder.all());
    }


    public Group getGroup(Long groupId) throws InvalidArgumentException, UnauthorizedException {
        Optional<Group> groupOptional = groupFinder.byIdOptional(groupId);
        if (!groupOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new InvalidArgumentException("Diese Gruppe existiert nicht.");
        }

        Group group = groupOptional.get();

        if(!policy.CanViewGroupDetails(sessionManager.currentUser(), group))
            throw new UnauthorizedException("Du bist nicht authorisiert auf diese Gruppe zuzugreifen.");

        return group;
    }

    public Set<User> getUsersWhichAreNotInThisGroup(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Group group = getGroup(groupId);

        if (!Policy.instance.CanViewGroupDetails(sessionManager.currentUser(), group)) {
            throw new UnauthorizedException("Du bist nicht authorisiert, die Mitglieder dieser Gruppe zu sehen.");
        }

        return userFinder.query().where().notIn("userId",
                group.getMembers().stream().map(user -> user.getUserId()).collect(Collectors.toSet())
        ).findSet();
    }

    public void removeGroupMember(Long userToRemove, Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Optional<User> tobeRemovedUserOptional = userFinder.byIdOptional(userToRemove);
        Optional<Group> groupOptional = groupFinder.byIdOptional(groupId);

        if (!groupOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new InvalidArgumentException("Diese Gruppe existiert nicht.");
        }
        if (!tobeRemovedUserOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new InvalidArgumentException("Dieser User kann nicht geloescht werden, weil er nicht existiert.");
        }

        Group g = groupOptional.get();
        User toBeRemovedUser = tobeRemovedUserOptional.get();

        if(!Policy.instance.CanRemoveGroupMember(sessionManager.currentUser(), g, toBeRemovedUser)) {
            throw new UnauthorizedException("Du bist nicht authorisiert, einen Member aus dieser Gruppe zu loeschen.");
        }

        g.getMembers().remove(toBeRemovedUser);
        ebeanServer.save(g);
    }

    public void addGroupMember(Long userToAdd, Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Optional<User> tobeAddedUserOptional = userFinder.byIdOptional(userToAdd);
        Optional<Group> groupOptional = groupFinder.byIdOptional(groupId);

        if (!groupOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new InvalidArgumentException("Diese Gruppe existiert nicht.");
        }
        if (!tobeAddedUserOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new InvalidArgumentException("Dieser User kann nicht hinzugefuegt werden, weil er nicht existiert.");
        }

        Group g = groupOptional.get();
        User toBeAddedUser = tobeAddedUserOptional.get();

        if (!Policy.instance.CanAddSpecificGroupMember(sessionManager.currentUser(), g, toBeAddedUser)) {
            throw new UnauthorizedException("Du bist nicht authorisiert, einen Member zu dieser Gruppe hinzu zu fuegen.");
        }

        g.getMembers().add(toBeAddedUser);
        ebeanServer.save(g);
    }

    public void deleteGroup(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Optional<Group> groupOptional = groupFinder.byIdOptional(groupId);
        if (!groupOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new InvalidArgumentException("Diese Gruppe existiert nicht.");
        }

        Group g = groupOptional.get();
        if (!Policy.instance.CanDeleteGroup(sessionManager.currentUser(), g)) {
            throw new UnauthorizedException("Du bist nicht authorisiert, eine Gruppe zu loeschen.");
        }

        ebeanServer.delete(g);
    }

}
