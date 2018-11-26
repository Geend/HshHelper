package managers.groupmanager;

import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import managers.filemanager.FileMetaFactory;
import managers.filemanager.dto.FileMeta;
import models.File;
import models.Group;
import models.User;
import models.finders.FileFinder;
import models.finders.GroupFinder;
import models.finders.UserFinder;
import play.Logger;
import policyenforcement.Policy;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

public class GroupManager {

    private final GroupFinder groupFinder;
    private final UserFinder userFinder;
    private final EbeanServer ebeanServer;
    private final SessionManager sessionManager;
    private final FileFinder fileFinder;
    private final Policy policy;
    private final FileMetaFactory fileMetaFactory;

    private static final Logger.ALogger logger = Logger.of(GroupManager.class);

    @Inject
    public GroupManager(GroupFinder groupFinder, UserFinder userFinder, EbeanServer ebeanServer, SessionManager sessionManager, Policy policy, FileFinder fileFinder, FileMetaFactory fileMetaFactory) {
        this.fileFinder = fileFinder;
        this.groupFinder = groupFinder;
        this.userFinder = userFinder;
        this.ebeanServer = ebeanServer;
        this.sessionManager = sessionManager;
        this.policy = policy;
        this.fileMetaFactory = fileMetaFactory;
    }

    public void createGroup(String groupName) throws GroupNameAlreadyExistsException, InvalidArgumentException {
        User user = sessionManager.currentUser();

        try(Transaction tx = ebeanServer.beginTransaction(TxIsolation.REPEATABLE_READ)) {
            Optional<Group> txGroup = groupFinder.byName(groupName);
            if(txGroup.isPresent()) {
                logger.error(user + "tried to create group" + groupName + " but that name already exists");
                throw new GroupNameAlreadyExistsException("Gruppe existiert bereits.");
            }

            Group group = new Group(groupName, user);
            group.getMembers().add(user);
            ebeanServer.save(group);

            tx.commit();
            logger.info(user + "created group" + groupName);
        }
    }

    public List<FileMeta> getGroupFiles(Group group) throws UnauthorizedException, InvalidArgumentException {
        return fileMetaFactory.fromFiles(
            fileFinder.query()
            .where()
            .and()
            .eq("groupPermissions.group", group)
            .or()
            .eq("groupPermissions.canRead", true)
            .eq("groupPermissions.canWrite", true)
            .endOr()
            .endAnd()
            .findList()
        );
    }

    public List<Group> getAllGroups() throws InvalidArgumentException, UnauthorizedException {
        if(!Policy.instance.CanSeeAllGroups(sessionManager.currentUser()))
            throw new UnauthorizedException();

        logger.info(sessionManager.currentUser().getUsername() + " is looking at all groups.");

        return groupFinder.all();
    }


    public Group getGroup(Long groupId) throws InvalidArgumentException, UnauthorizedException {
        Optional<Group> groupOptional = groupFinder.byIdOptional(groupId);
        if (!groupOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new InvalidArgumentException("Diese Gruppe existiert nicht.");
        }

        Group group = groupOptional.get();

        if(!policy.CanViewGroupDetails(sessionManager.currentUser(), group)) {
            logger.error(sessionManager.currentUser().getUsername() + " tried to access group " + group.getName() + " for which he is not authorized");
            throw new UnauthorizedException("Du bist nicht authorisiert auf diese Gruppe zuzugreifen.");
        }

        return group;
    }

    public List<User> getUsersWhichAreNotInThisGroup(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Group group = getGroup(groupId);

        if (!Policy.instance.CanViewGroupDetails(sessionManager.currentUser(), group)) {
            logger.error(sessionManager.currentUser().getUsername() + " tried to see the members of group " + group.getName() + " for which he is not authorized");
            throw new UnauthorizedException("Du bist nicht authorisiert, die Mitglieder dieser Gruppe zu sehen.");
        }

        return userFinder.query().where().notIn("userId",
                group.getMembers().stream().map(user -> user.getUserId()).collect(Collectors.toSet())
        ).findList();
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
            logger.error(sessionManager.currentUser().getUsername() + " tried to delete + " + toBeRemovedUser.getUsername() + " from group " + g.getName() + " but he is not authorized");
            throw new UnauthorizedException("Du bist nicht authorisiert, einen Member aus dieser Gruppe zu loeschen.");
        }

        g.getMembers().remove(toBeRemovedUser);
        ebeanServer.save(g);

        logger.info(sessionManager.currentUser().getUsername() + " deleted " + toBeRemovedUser.getUsername() + " from group " + g.getName());
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
            logger.error(sessionManager.currentUser().getUsername() + " tried to add + " + toBeAddedUser.getUsername() + " to group " + g.getName() + " but he is not authorized");
            throw new UnauthorizedException("Du bist nicht authorisiert, einen Member zu dieser Gruppe hinzu zu fuegen.");
        }

        g.getMembers().add(toBeAddedUser);
        ebeanServer.save(g);

        logger.info(sessionManager.currentUser().getUsername() + " added " + toBeAddedUser.getUsername() + " to group " + g.getName());
    }

    public void deleteGroup(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Optional<Group> groupOptional = groupFinder.byIdOptional(groupId);
        if (!groupOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new InvalidArgumentException("Diese Gruppe existiert nicht.");
        }

        Group g = groupOptional.get();
        if (!Policy.instance.CanDeleteGroup(sessionManager.currentUser(), g)) {
            logger.error(sessionManager.currentUser().getUsername() + " tried to delete group " + g.getName() + " but he is not authorized");
            throw new UnauthorizedException("Du bist nicht authorisiert, eine Gruppe zu loeschen.");
        }

        ebeanServer.delete(g);
        logger.info(sessionManager.currentUser().getUsername() + " deleted group " + g.getName());
    }

}
