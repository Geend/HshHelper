package controllers;

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
import play.mvc.Result;
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

    public Result createGroup() {
        Form<CreateGroupDto> bf = groupForm.bindFromRequest();

        if(bf.hasErrors()) {
            return badRequest(views.html.CreateGroup.render(bf));
        } else {
            CreateGroupDto gDto = bf.get();

            try {
                groupManager.createGroup(sessionManager.currentUser().getUserId(), gDto.getName());
            } catch (GroupNameAlreadyExistsException e) {
                bf = bf.withError("name", e.getMessage());
                return badRequest(views.html.CreateGroup.render(bf));
            } catch (IllegalArgumentException e) {
                return badRequest(e.getMessage());
            }

            return redirect(routes.GroupController.showOwnGroups());
        }
    }

    public Result showOwnGroups() {
        Set<Group> gms = groupManager.getOwnGroups(sessionManager.currentUser().getUserId());
        return ok(views.html.OwnGroupsList.render(asScala(gms), deleteGroupForm));
    }

    public Result showGroup(Long groupId) {
        try {
            Set<User> members = groupManager.getGroupMembers(sessionManager.currentUser().getUserId(), groupId);
            Set<User> notMember = groupManager.getUsersWhichAreNotInThisGroup(sessionManager.currentUser().getUserId(), groupId);
            Group grp = groupManager.getGroup(sessionManager.currentUser().getUserId(), groupId);
            return ok(views.html.GroupMembersList.render(grp,
                            asScala(members), asScala(notMember), addUserToGroupForm, removeGroupUserForm));
        } catch (UnauthorizedException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            return internalServerError(e.getMessage());
        }
    }

    public Result removeGroupMember(Long groupId) {
        Form<UserIdDto> form = removeGroupUserForm.bindFromRequest();
        if(form.hasErrors()) {
            return badRequest("error");
        }

        UserIdDto ru = form.get();

        try {
            groupManager.removeGroupMember(sessionManager.currentUser().getUserId(), ru.getUserId(), groupId);
        } catch (UnauthorizedException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }

        return redirect(routes.GroupController.showGroup(groupId));
    }

    public Result addGroupMember(Long groupId) {
        Form<UserIdDto> form = addUserToGroupForm.bindFromRequest();
        if(form.hasErrors()) {
            return badRequest("error");
        }

        UserIdDto au = form.get();

        try {
            groupManager.addGroupMember(sessionManager.currentUser().getUserId(), au.getUserId(), groupId);
        } catch (UnauthorizedException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }

        return redirect(routes.GroupController.showGroup(groupId));
    }

    public Result deleteGroup(Long groupId) {
        Form<DeleteGroupDto> form = deleteGroupForm.bindFromRequest();
        if(form.hasErrors()) {
            return badRequest("error");
        }

        DeleteGroupDto dg = form.get();

        try {
            groupManager.deleteGroup(sessionManager.currentUser().getUserId(), groupId);
        } catch (UnauthorizedException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }

        return redirect(routes.GroupController.showOwnGroups());
    }
}
