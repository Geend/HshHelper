package controllers;

import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.groupmanager.GroupManager;
import managers.groupmanager.GroupNameAlreadyExistsException;
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
import java.util.Map;
import java.util.Set;
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

    @Inject
    public GroupController(FormFactory formFactory, UserFinder userFinder, GroupFinder groupFinder, GroupManager groupManager, SessionManager sessionManager) {
        this.groupForm = formFactory.form(CreateGroupDto.class);
        this.removeGroupUserForm = formFactory.form(UserIdDto.class);
        this.addUserToGroupForm = formFactory.form(UserIdDto.class);
        this.deleteGroupForm = formFactory.form(DeleteGroupDto.class);
        this.groupManager = groupManager;
        this.sessionManager = sessionManager;
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

            return redirect(routes.GroupController.showOwnGroups());
        }
    }

    public Result showOwnGroups() {
        Map<Group, Form<DeleteGroupDto>> groupFormMap = createGroupsFormMap(
                sessionManager.currentUser().getGroups()
        );
        return ok(views.html.groups.OwnGroupsList.render(asScala(groupFormMap)));
    }

    public Result showAllGroups() throws InvalidArgumentException, UnauthorizedException {
        Map<Group, Form<DeleteGroupDto>> groupFormMap = createGroupsFormMap(
                groupManager.getAllGroups()
        );
        return ok(views.html.groups.AllGroupsList.render(asScala(groupFormMap)));
    }

    public Result showGroup(Long groupId) throws UnauthorizedException {
        return renderGroupMemberList(groupId, addUserToGroupForm, removeGroupUserForm, Http.Status.OK);
    }

    private Result renderGroupMemberList(Long groupId, Form<UserIdDto> addUserToGroupForm, Form<UserIdDto> removeGroupUserForm, int httpStatus) throws UnauthorizedException {

        try {
            Map<User, Form<UserIdDto>> membersFormMap =
                    groupManager.getGroup(groupId).getMembers()
                    .stream()
                    .collect(Collectors.toMap(
                            user -> user,
                            u -> removeGroupUserForm.fill(new UserIdDto(u.getUserId()))
                            ));
            Set<User> notMember = groupManager.getUsersWhichAreNotInThisGroup(groupId);
            Group grp = groupManager.getGroup(groupId);
            return status(httpStatus, views.html.groups.GroupMembersList.render(grp,
                    asScala(membersFormMap), asScala(notMember), addUserToGroupForm, removeGroupUserForm));
        } catch (IllegalArgumentException | UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            return internalServerError(e.getMessage());
        }
    }

    public Result removeGroupMember(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Form<UserIdDto> form = removeGroupUserForm.bindFromRequest();
        if (form.hasErrors()) {
            return renderGroupMemberList(groupId, addUserToGroupForm, form, Http.Status.BAD_REQUEST);
        }

        UserIdDto ru = form.get();


        groupManager.removeGroupMember(ru.getUserId(), groupId);


        return redirect(routes.GroupController.showGroup(groupId));
    }

    public Result addGroupMember(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Form<UserIdDto> form = addUserToGroupForm.bindFromRequest();
        if (form.hasErrors()) {
            return renderGroupMemberList(groupId, form, removeGroupUserForm, Http.Status.BAD_REQUEST);
        }

        UserIdDto au = form.get();


        groupManager.addGroupMember(au.getUserId(), groupId);

        return redirect(routes.GroupController.showGroup(groupId));
    }

    public Result deleteOwnGroup(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Form<DeleteGroupDto> form = deleteGroupForm.bindFromRequest();

        if (form.hasErrors()) {
            Map<Group, Form<DeleteGroupDto>> groupFormMap = createGroupsFormMap(
                sessionManager.currentUser().getGroups()
            );
            return badRequest(views.html.groups.OwnGroupsList.render(asScala(groupFormMap)));
        }

        DeleteGroupDto dg = form.get();
        groupManager.deleteGroup(groupId);

        return redirect(routes.GroupController.showOwnGroups());
    }

    public Result deleteGroup(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Form<DeleteGroupDto> form = deleteGroupForm.bindFromRequest();

        if (form.hasErrors()) {
            Map<Group, Form<DeleteGroupDto>> groupFormMap = createGroupsFormMap(
                    sessionManager.currentUser().getGroups()
            );
            return badRequest(views.html.groups.OwnGroupsList.render(asScala(groupFormMap)));
        }

        groupManager.deleteGroup(groupId);

        return redirect(routes.GroupController.showAllGroups());
    }

    private Map<Group, Form<DeleteGroupDto>> createGroupsFormMap(Set<Group> groups) {
        return groups
                .stream()
                .collect(Collectors.toMap(
                        gr -> gr,
                        group -> deleteGroupForm.fill(new DeleteGroupDto(group.getGroupId()))
                ));
    }
}
