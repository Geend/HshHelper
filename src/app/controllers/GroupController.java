package controllers;

import extension.AuthenticatedController;
import models.Group;
import models.GroupMembership;
import models.dtos.CreateGroupDTO;
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
public class GroupController extends AuthenticatedController {
    private final Form<CreateGroupDTO> groupForm;

    @Inject
    public GroupController(FormFactory formFactory) {
        this.groupForm = formFactory.form(CreateGroupDTO.class);
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
            group.ownerId = getCurrentUser().id;
            Group.addGroup(group);

            GroupMembership gm = new GroupMembership();
            gm.groupId = group.id;
            gm.userId = getCurrentUser().id;
            GroupMembership.add(gm);

            return redirect(routes.GroupController.getOwnGroups());
        }
    }

    public Result getOwnGroups() {
        Set<Integer> gms = GroupMembership.findAll().stream().filter(x -> x.userId == getCurrentUser().id).map(x -> x.groupId).collect(Collectors.toUnmodifiableSet());
        List<Group> groups = Group.findAll().stream().filter(x -> gms.contains(x.id)).collect(Collectors.toList());

        return ok(views.html.OwnGroupsList.render(asScala(groups)));
    }
}
