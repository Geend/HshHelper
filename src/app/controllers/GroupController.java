package controllers;

import extension.AuthenticationRequired;
import extension.ContextArguments;
import models.Group;
import models.User;
import models.dtos.AddUserToGroupDTO;
import models.dtos.CreateGroupDTO;
import models.dtos.DeleteGroupDTO;
import models.dtos.RemoveGroupUserDTO;
import models.finders.GroupFinder;
import models.finders.UserFinder;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static play.libs.Scala.asScala;

@Singleton
@AuthenticationRequired
public class GroupController extends Controller {
    private final Form<CreateGroupDTO> groupForm;
    private final Form<RemoveGroupUserDTO> removeGroupUserForm;
    private final UserFinder userFinder;
    private final Form<AddUserToGroupDTO> addUserToGroupForm;
    private final Form<DeleteGroupDTO> deleteGroupForm;
    private final GroupFinder groupFinder;

    @Inject
    public GroupController(FormFactory formFactory, UserFinder userFinder, GroupFinder groupFinder) {
        this.groupForm = formFactory.form(CreateGroupDTO.class);
        this.removeGroupUserForm = formFactory.form(RemoveGroupUserDTO.class);
        this.userFinder = userFinder;
        this.groupFinder = groupFinder;
        this.addUserToGroupForm = formFactory.form(AddUserToGroupDTO.class);
        this.deleteGroupForm = formFactory.form(DeleteGroupDTO.class);
    }

    public Result showCreateGroupForm() {
        return ok(views.html.CreateGroup.render(groupForm));
    }

    public Result createGroup() {
        Form<CreateGroupDTO> bf = groupForm.bindFromRequest();

        if(bf.hasErrors()) {
            return badRequest(views.html.CreateGroup.render(bf));
        } else {
            CreateGroupDTO gDto = bf.get();

            Group group = new Group(gDto.getName(), ContextArguments.getUser().get());
            group.getMembers().add( ContextArguments.getUser().get());
            group.save();

            return redirect(routes.GroupController.showOwnGroups());
        }
    }

    public Result showOwnGroups() {
        Set<Group> gms = ContextArguments.getUser().get().getGroups();
        return ok(views.html.OwnGroupsList.render(asScala(gms), deleteGroupForm));
    }

    public Result showGroup(Long groupId) {
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
        Form<RemoveGroupUserDTO> form = removeGroupUserForm.bindFromRequest();
        if(form.hasErrors()) {
            return badRequest("error");
        }

        RemoveGroupUserDTO ru = form.get();

        User toBeDeleted = userFinder.byIdOptional(ru.getUserId()).get();
        Group g = groupFinder.byIdOptional(groupId).get();
        if(!policy.Specification.CanRemoveGroupMember(ContextArguments.getUser().get(), g, toBeDeleted)) {
            return badRequest("error");
        }
        
        g.getMembers().remove(toBeDeleted);
        g.save();

        return redirect(routes.GroupController.showGroup(groupId));
    }

    public Result addGroupMember(Long groupId) {
        Form<AddUserToGroupDTO> form = addUserToGroupForm.bindFromRequest();
        if(form.hasErrors()) {
            return badRequest("error");
        }

        AddUserToGroupDTO au = form.get();

        User toBeAdded= userFinder.byIdOptional(au.getUserId()).get();
        Group g = groupFinder.byIdOptional(groupId).get();
        if(!policy.Specification.CanAddGroupMember(ContextArguments.getUser().get(), g,
                toBeAdded)) {
            return badRequest("error");
        }

        g.getMembers().add(toBeAdded);
        g.save();

        return redirect(routes.GroupController.showGroup(groupId));
    }

    public Result deleteGroup(Long groupId) {
        Form<DeleteGroupDTO> form = deleteGroupForm.bindFromRequest();
        if(form.hasErrors()) {
            return badRequest("error");
        }

        DeleteGroupDTO dg = form.get();
        Group g = groupFinder.byIdOptional(groupId).get();

        if(!policy.Specification.CanDeleteGroup(ContextArguments.getUser().get(), g)) {
            return badRequest("error");
        }

        g.delete();

        return redirect(routes.GroupController.showOwnGroups());
    }
}
