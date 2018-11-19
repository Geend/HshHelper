package controllers;

import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.filemanager.FileManager;
import managers.groupmanager.GroupManager;
import managers.groupmanager.GroupNameAlreadyExistsException;
import models.File;
import models.Group;
import models.User;
import dtos.CreateGroupDto;
import dtos.DeleteGroupDto;
import dtos.UserIdDto;
import models.finders.GroupFinder;
import models.finders.UserFinder;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import policyenforcement.session.Authentication;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

import static play.libs.Scala.asScala;

@Singleton
@Authentication.Required
public class GroupController extends Controller {
    private final Form<CreateGroupDto> groupForm;
    private final Form<UserIdDto> removeGroupUserForm;
    private final Form<UserIdDto> addUserToGroupForm;
    private final Form<DeleteGroupDto> deleteGroupForm;

    private final GroupManager groupManager;
    private final SessionManager sessionManager;
    private final FileManager fileManager;

    @Inject
    public GroupController(FormFactory formFactory, GroupManager groupManager, SessionManager sessionManager, FileManager fileManager) {
        this.groupForm = formFactory.form(CreateGroupDto.class);
        this.removeGroupUserForm = formFactory.form(UserIdDto.class);
        this.addUserToGroupForm = formFactory.form(UserIdDto.class);
        this.deleteGroupForm = formFactory.form(DeleteGroupDto.class);
        this.groupManager = groupManager;
        this.sessionManager = sessionManager;
        this.fileManager = fileManager;
    }

    public Result showAddMemberForm(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Group group = groupManager.getGroup(groupId);
        List<User> notMember = groupManager.getUsersWhichAreNotInThisGroup(groupId);
        return ok(views.html.groups.GroupAddMember.render(group, asScala(notMember), addUserToGroupForm));
    }

    public Result addGroupMember(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Form<UserIdDto> form = addUserToGroupForm.bindFromRequest();
        if (form.hasErrors()) {
            Group group = groupManager.getGroup(groupId);
            List<User> notMember = groupManager.getUsersWhichAreNotInThisGroup(groupId);
            return badRequest(views.html.groups.GroupAddMember.render(group, asScala(notMember), form));
        }

        UserIdDto au = form.get();

        groupManager.addGroupMember(au.getUserId(), groupId);

        return redirect(routes.GroupController.showGroupMembers(groupId));
    }

    public Result showCreateGroupForm() {
        return ok(views.html.groups.CreateGroup.render(groupForm));
    }

    public Result createGroup() throws InvalidArgumentException {
        Form<CreateGroupDto> bf = groupForm.bindFromRequest();

        if (bf.hasErrors()) {
            return badRequest(views.html.groups.CreateGroup.render(bf));
        } else {
            CreateGroupDto gDto = bf.get();

            try {
                groupManager.createGroup(gDto.getName());
            } catch (GroupNameAlreadyExistsException e) {
                bf = bf.withError("name", e.getMessage());
                return badRequest(views.html.groups.CreateGroup.render(bf));
            }

            return redirect(routes.GroupController.showOwnMemberships());
        }
    }

    public Result showOwnMemberships() {
        User user = sessionManager.currentUser();
        return ok(views.html.groups.Groups.render(asScala(user.getGroups())));
    }

    public Result showOwnGroups() {
        User user = sessionManager.currentUser();
        return ok(views.html.groups.Groups.render(asScala(user.getOwnerOf())));
    }

    public Result showAllGroups() throws InvalidArgumentException, UnauthorizedException {
        List<Group> groups = groupManager.getAllGroups();
        return ok(views.html.groups.Groups.render(asScala(groups)));
    }

    public Result showGroup(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        return redirect(routes.GroupController.showGroupFiles(groupId));
    }

    public Result showGroupFiles(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Group group = groupManager.getGroup(groupId);
        List<File> sharedWithGroup = fileManager.getGroupFiles(group);
        return ok(views.html.groups.GroupFiles.render(group, asScala(sharedWithGroup)));
    }

    public Result showGroupMembers(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Group group = groupManager.getGroup(groupId);
        List<User> members = group.getMembers();
        return ok(views.html.groups.GroupMembers.render(group, asScala(members)));
    }

    public Result deleteGroup() throws UnauthorizedException, InvalidArgumentException {
        Form<DeleteGroupDto> form = deleteGroupForm.bindFromRequest();

        if (form.hasErrors()) {
            return badRequest();
        }

        groupManager.deleteGroup(form.get().getGroupId());

        return redirect(routes.GroupController.showOwnMemberships());
    }

    public Result removeGroupMember(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Form<UserIdDto> form = removeGroupUserForm.bindFromRequest();
        if (form.hasErrors()) {
            return badRequest();
        }

        UserIdDto ru = form.get();

        groupManager.removeGroupMember(ru.getUserId(), groupId);

        return redirect(routes.GroupController.showGroupMembers(groupId));
    }
}
