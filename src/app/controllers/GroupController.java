package controllers;

import domainlogic.InvalidArgumentException;
import domainlogic.UnauthorizedException;
import domainlogic.groupmanager.GroupManager;
import domainlogic.groupmanager.GroupNameAlreadyExistsException;
import models.Group;
import models.User;
import models.dtos.CreateGroupDto;
import models.dtos.DeleteGroupDto;
import models.dtos.UserIdDto;
import models.finders.GroupFinder;
import models.finders.UserFinder;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import policy.session.Authentication;
import policy.session.SessionManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

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
        return ok(views.html.CreateGroup.render(groupForm));
    }

    public Result createGroup() throws InvalidArgumentException {
        Form<CreateGroupDto> bf = groupForm.bindFromRequest();

        if (bf.hasErrors()) {
            return badRequest(views.html.CreateGroup.render(bf));
        } else {
            CreateGroupDto gDto = bf.get();

            try {
                groupManager.createGroup(sessionManager.currentUser().getUserId(), gDto.getName());
            } catch (GroupNameAlreadyExistsException e) {
                bf = bf.withError("name", e.getMessage());
                return badRequest(views.html.CreateGroup.render(bf));
            }

            return redirect(routes.GroupController.showOwnGroups());
        }
    }

    public Result showOwnGroups() throws InvalidArgumentException {
        Set<Group> gms = groupManager.getOwnGroups(sessionManager.currentUser().getUserId());
        return ok(views.html.OwnGroupsList.render(asScala(gms), deleteGroupForm));
    }

    public Result showAllGroups() throws InvalidArgumentException, UnauthorizedException {
        Set<Group> gms = groupManager.getAllGroups(sessionManager.currentUser().getUserId());
        return ok(views.html.AllGroupsList.render(asScala(gms), deleteGroupForm));
    }

    public Result showGroup(Long groupId) throws UnauthorizedException {
        return renderGroupMemberList(groupId, addUserToGroupForm, removeGroupUserForm, Http.Status.OK);
    }

    private Result renderGroupMemberList(Long groupId, Form<UserIdDto> addUserToGroupForm, Form<UserIdDto> removeGroupUserForm, int httpStatus) throws UnauthorizedException {

        try {
            Set<User> members = groupManager.getGroupMembers(sessionManager.currentUser().getUserId(), groupId);
            Set<User> notMember = groupManager.getUsersWhichAreNotInThisGroup(sessionManager.currentUser().getUserId(), groupId);
            Group grp = groupManager.getGroup(sessionManager.currentUser().getUserId(), groupId);
            return status(httpStatus, views.html.GroupMembersList.render(grp,
                    asScala(members), asScala(notMember), addUserToGroupForm, removeGroupUserForm));
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


        groupManager.removeGroupMember(sessionManager.currentUser().getUserId(), ru.getUserId(), groupId);


        return redirect(routes.GroupController.showGroup(groupId));
    }

    public Result addGroupMember(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Form<UserIdDto> form = addUserToGroupForm.bindFromRequest();
        if (form.hasErrors()) {
            return renderGroupMemberList(groupId, form, removeGroupUserForm, Http.Status.BAD_REQUEST);
        }

        UserIdDto au = form.get();


        groupManager.addGroupMember(sessionManager.currentUser().getUserId(), au.getUserId(), groupId);

        return redirect(routes.GroupController.showGroup(groupId));
    }

    public Result deleteGroup(Long groupId) throws UnauthorizedException, InvalidArgumentException {
        Form<DeleteGroupDto> form = deleteGroupForm.bindFromRequest();

        if (form.hasErrors()) {
            Set<Group> gms = groupManager.getOwnGroups(sessionManager.currentUser().getUserId());
            return badRequest(views.html.OwnGroupsList.render(asScala(gms), form));
        }

        DeleteGroupDto dg = form.get();
        groupManager.deleteGroup(sessionManager.currentUser().getUserId(), groupId);

        return redirect(routes.GroupController.showOwnGroups());
    }
}
