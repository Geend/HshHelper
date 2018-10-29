package controllers;

import io.ebean.Ebean;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import models.Group;
import models.User;
import models.dtos.AddUserToGroupDto;
import models.dtos.CreateGroupDto;
import models.dtos.DeleteGroupDto;
import models.dtos.RemoveGroupUserDto;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static play.libs.Scala.asScala;

@Singleton
@Authentication.Required
public class GroupController extends Controller {
    private final Form<CreateGroupDto> groupForm;
    private final Form<RemoveGroupUserDto> removeGroupUserForm;
    private final UserFinder userFinder;
    private final Form<AddUserToGroupDto> addUserToGroupForm;
    private final Form<DeleteGroupDto> deleteGroupForm;
    private final GroupFinder groupFinder;

    @Inject
    public GroupController(FormFactory formFactory, UserFinder userFinder, GroupFinder groupFinder) {
        this.groupForm = formFactory.form(CreateGroupDto.class);
        this.removeGroupUserForm = formFactory.form(RemoveGroupUserDto.class);
        this.userFinder = userFinder;
        this.groupFinder = groupFinder;
        this.addUserToGroupForm = formFactory.form(AddUserToGroupDto.class);
        this.deleteGroupForm = formFactory.form(DeleteGroupDto.class);
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

            try(Transaction tx = Ebean.beginTransaction(TxIsolation.REPEATABLE_READ)) {
                Optional<Group> txGroup = groupFinder.byName(gDto.getName());
                if(txGroup.isPresent()) {
                    bf = bf.withError("name", "Existiert bereits!");
                    return badRequest(views.html.CreateGroup.render(bf));
                }

                Group group = new Group(gDto.getName(), SessionManager.CurrentUser());
                group.getMembers().add(SessionManager.CurrentUser());
                group.save();

                tx.commit();
            }

            return redirect(routes.GroupController.showOwnGroups());
        }
    }

    public Result showOwnGroups() {
        Set<Group> gms = SessionManager.CurrentUser().getGroups();
        return ok(views.html.OwnGroupsList.render(asScala(gms), deleteGroupForm));
    }

    public Result showGroup(Long groupId) {
        // TODO: Policy Check fehlt offensichtlich
        Optional<Group> g = groupFinder.byIdOptional(groupId);

        if(!g.isPresent())
            return notFound("404");
        List<User> notMember = userFinder.all().stream().filter(
                user -> !g.get().getMembers().contains(user))
                .collect(Collectors.toList());
        return g.map(grp ->
                ok(views.html.GroupMembersList.render(grp,
                        asScala(grp.getMembers()), asScala(notMember), addUserToGroupForm, removeGroupUserForm)))
                .get();
    }

    public Result removeGroupMember(Long groupId) {
        Form<RemoveGroupUserDto> form = removeGroupUserForm.bindFromRequest();
        if(form.hasErrors()) {
            return badRequest("error");
        }

        RemoveGroupUserDto ru = form.get();

        User toBeDeleted = userFinder.byIdOptional(ru.getUserId()).get();
        Group g = groupFinder.byIdOptional(groupId).get();
        if(!policy.Specification.CanRemoveGroupMember(SessionManager.CurrentUser(), g, toBeDeleted)) {
            return badRequest("error");
        }
        
        g.getMembers().remove(toBeDeleted);
        g.save();

        return redirect(routes.GroupController.showGroup(groupId));
    }

    public Result addGroupMember(Long groupId) {
        Form<AddUserToGroupDto> form = addUserToGroupForm.bindFromRequest();
        if(form.hasErrors()) {
            return badRequest("error");
        }

        AddUserToGroupDto au = form.get();

        User toBeAdded= userFinder.byIdOptional(au.getUserId()).get();
        Group g = groupFinder.byIdOptional(groupId).get();
        if(!policy.Specification.CanAddGroupMember(SessionManager.CurrentUser(), g, toBeAdded)) {
            return badRequest("error");
        }

        g.getMembers().add(toBeAdded);
        g.save();

        return redirect(routes.GroupController.showGroup(groupId));
    }

    public Result deleteGroup(Long groupId) {
        Form<DeleteGroupDto> form = deleteGroupForm.bindFromRequest();
        if(form.hasErrors()) {
            return badRequest("error");
        }

        DeleteGroupDto dg = form.get();
        Group g = groupFinder.byIdOptional(groupId).get();

        if(!policy.Specification.CanDeleteGroup(SessionManager.CurrentUser(), g)) {
            return badRequest("error");
        }

        g.delete();

        return redirect(routes.GroupController.showOwnGroups());
    }
}
