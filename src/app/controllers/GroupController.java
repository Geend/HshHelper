package controllers;

import extension.ContextArguments;
import models.Group;
import models.GroupMembership;
import models.User;
import models.dtos.CreateGroupDTO;
import models.dtos.RemoveGroupUserDTO;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static play.libs.Scala.asScala;

@Singleton
public class GroupController extends Controller {
    private final Form<CreateGroupDTO> groupForm;
    private final Form<RemoveGroupUserDTO> removeGroupUserForm;

    @Inject
    public GroupController(FormFactory formFactory) {
        this.groupForm = formFactory.form(CreateGroupDTO.class);
        this.removeGroupUserForm = formFactory.form(RemoveGroupUserDTO.class);
    }

    public Result getCreateGroup() {
        return ok(views.html.CreateGroup.render(groupForm));
    }

    public Result postCreateGroup() {
        Form<CreateGroupDTO> bf = groupForm.bindFromRequest();

        if(bf.hasErrors()) {
            return badRequest(views.html.CreateGroup.render(bf));
        } else {
            CreateGroupDTO gDto = bf.get();
            Group group = new Group();
            group.name = gDto.getName();
            group.ownerId = ContextArguments.getUser().get().id;
            Group.addGroup(group);

            GroupMembership gm = new GroupMembership();
            gm.groupId = group.id;
            gm.userId = ContextArguments.getUser().get().id;
            GroupMembership.add(gm);

            return redirect(routes.GroupController.getOwnGroups());
        }
    }

    public Result getOwnGroups() {
        Set<Integer> gms = GroupMembership.findAll().stream().filter(x -> x.userId == ContextArguments.getUser().get().id).map(x -> x.groupId).collect(Collectors.toSet());
        List<Group> groups = Group.findAll().stream().filter(x -> gms.contains(x.id)).collect(Collectors.toList());

        return ok(views.html.OwnGroupsList.render(asScala(groups)));
    }

    public Result getGroup(int id) {
        Group g = Group.getById(id);
        if(g == null)
            return notFound("404");
        List<User> users = GroupMembership.getGroupUsers(g);
        return ok(views.html.GroupMembersList.render(g, asScala(users)));
    }

    public Result postRemoveMember(int groupId) {
        Form<RemoveGroupUserDTO> form = removeGroupUserForm.bindFromRequest();
        if(form.hasErrors()) {
            return badRequest("error");
        }

        RemoveGroupUserDTO ru = form.get();

        User toBeDeleted = User.getById(ru.getUserId());
        Group g = Group.getById(groupId);
        if(!policy.Specification.CanRemoveGroupMemeber(ContextArguments.getUser().get(), g, toBeDeleted)) {
            return badRequest("error");
        }

        GroupMembership.remove(g, toBeDeleted);

        return redirect(routes.GroupController.getGroup(groupId));
    }

    public Result getAddMember(int groupId) {
        return ok("..");
    }
}
