package domainlogic.groupmanager;

import domainlogic.UnauthorizedException;
import models.Group;
import models.User;

import java.util.List;

public class GroupManager {
    public void createGroup(Long userId, String groupName) throws GroupNameAlreadyExistsException {

    }

    public List<Group> getOwnGroups(Long userId) {
        return null;
    }

    public List<User> getGroupMembers(Long userId, Long groupId) throws UnauthorizedException {
        return null;
    }

    public void removeGroupMember(Long userId, Long userToRemove, Long groupId) throws UnauthorizedException {

    }

    public void addGroupMember(Long userId, Long userToAdd, Long groupId) throws UnauthorizedException {

    }

    public void deleteGroup(Long userId, Long groupId) throws UnauthorizedException {

    }
}
