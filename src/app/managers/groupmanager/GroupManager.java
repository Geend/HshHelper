package managers.groupmanager;

import extension.logging.DangerousCharFilteringLogger;
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
    private final FileMetaFactory fileMetaFactory;

    private static final Logger.ALogger logger = new DangerousCharFilteringLogger(GroupManager.class);

    @Inject
    public GroupManager(GroupFinder groupFinder, UserFinder userFinder, EbeanServer ebeanServer, SessionManager sessionManager, FileFinder fileFinder, FileMetaFactory fileMetaFactory) {
        this.fileFinder = fileFinder;
        this.groupFinder = groupFinder;
        this.userFinder = userFinder;
        this.ebeanServer = ebeanServer;
        this.sessionManager = sessionManager;
        this.fileMetaFactory = fileMetaFactory;
    }

    public void createGroup(String groupName) throws GroupNameAlreadyExistsException, InvalidArgumentException {
        User user = sessionManager.currentUser();

        try(Transaction tx = ebeanServer.beginTransaction(TxIsolation.REPEATABLE_READ)) {
            Optional<Group> txGroup = groupFinder.byName(groupName);
            if(txGroup.isPresent()) {
                logger.error(user + " tried to create group " + groupName + " but that name already exists");
                throw new GroupNameAlreadyExistsException("Gruppe existiert bereits.");
            }

            Group group = new Group(groupName, user);
            group.getMembers().add(user);
            ebeanServer.save(group);

            tx.commit();
            logger.info(user + " created group " + group);
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
        if(!sessionManager.currentPolicy().canSeeAllGroups())
            throw new UnauthorizedException();

        logger.info(sessionManager.currentUser() + " is looking at all groups.");

        return groupFinder.all();
    }


    public Group getGroup(Long groupId) throws InvalidArgumentException, UnauthorizedException {
        Optional<Group> groupOptional = groupFinder.byIdOptional(groupId);
        if (!groupOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new InvalidArgumentException("Diese Gruppe existiert nicht.");
        }

        Group group = groupOptional.get();

        if(!sessionManager.currentPolicy().canViewGroupDetails(group)) {
            logger.error(sessionManager.currentUser() + " tried to access group " + group + " for which he is not authorized");
            throw new UnauthorizedException("Du bist nicht authorisiert auf diese Gruppe zuzugreifen.");
        }

        return group;
    }

    public List<User> getUsersWhichAreNotInThisGroup(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Group group = getGroup(groupId);

        if (!sessionManager.currentPolicy().canViewGroupDetails(group)) {
            logger.error(sessionManager.currentUser() + " tried to see the members of group " + group + " for which he is not authorized");
            throw new UnauthorizedException("Du bist nicht authorisiert, die Mitglieder dieser Gruppe zu sehen.");
        }

        return userFinder.query().where().notIn("userId",
                group.getMembers().stream().map(User::getUserId).collect(Collectors.toSet())
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

        if(!sessionManager.currentPolicy().canRemoveGroupMember(g, toBeRemovedUser)) {
            logger.error(sessionManager.currentUser() + " tried to delete + " + toBeRemovedUser + " from group " + g + " but he is not authorized");
            throw new UnauthorizedException("Du bist nicht authorisiert, einen Member aus dieser Gruppe zu loeschen.");
        }

        g.getMembers().remove(toBeRemovedUser);
        ebeanServer.save(g);

        logger.info(sessionManager.currentUser() + " deleted " + toBeRemovedUser + " from group " + g);
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

        if (!sessionManager.currentPolicy().canAddSpecificGroupMember(g, toBeAddedUser)) {
            logger.error(sessionManager.currentUser() + " tried to add + " + toBeAddedUser + " to group " + g + " but he is not authorized");
            throw new UnauthorizedException("Du bist nicht authorisiert, einen Member zu dieser Gruppe hinzu zu fuegen.");
        }

        g.getMembers().add(toBeAddedUser);
        ebeanServer.save(g);

        logger.info(sessionManager.currentUser() + " added " + toBeAddedUser + " to group " + g);
    }

    public void deleteGroup(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Optional<Group> groupOptional = groupFinder.byIdOptional(groupId);
        if (!groupOptional.isPresent()) {
            // TODO: change into a correct exception.
            throw new InvalidArgumentException("Diese Gruppe existiert nicht.");
        }

        Group g = groupOptional.get();

        if (!sessionManager.currentPolicy().canDeleteGroup(g)) {
            logger.error(sessionManager.currentUser() + " tried to delete group " + g + " but he is not authorized");
            throw new UnauthorizedException("Du bist nicht authorisiert, eine Gruppe zu loeschen.");
        }

        ebeanServer.delete(g);
        logger.info(sessionManager.currentUser() + " deleted group " + g);
    }

}
