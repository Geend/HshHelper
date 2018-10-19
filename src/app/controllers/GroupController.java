package controllers;

import extension.ContextArguments;
import models.Group;
import models.User;
import models.dtos.CreateGroupDTO;
import models.dtos.RemoveGroupUserDTO;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.Set;

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

            Group group = new Group(gDto.getName(), ContextArguments.getUser().get());
            group.members.add( ContextArguments.getUser().get());
            group.save();

            return redirect(routes.GroupController.getOwnGroups());
        }
    }

    public Result getOwnGroups() {
        Set<Group> gms = ContextArguments.getUser().get().groups;
        return ok(views.html.OwnGroupsList.render(asScala(gms)));
    }

    public Result getGroup(Long id) {
        Optional<Group> g = Group.find.byIdOptional(id);
        if(!g.isPresent())
            return notFound("404");
        return g.map(grp ->
                ok(views.html.GroupMembersList.render(grp,
                        asScala(grp.members))))
                .get();
    }

    public Result postRemoveMember(Long groupId) {
        Form<RemoveGroupUserDTO> form = removeGroupUserForm.bindFromRequest();
        if(form.hasErrors()) {
            return badRequest("error");
        }

        RemoveGroupUserDTO ru = form.get();

        User toBeDeleted = User.find.byIdOptional(ru.getUserId()).get();
        Group g = Group.find.byIdOptional(groupId).get();
        if(!policy.Specification.CanRemoveGroupMember(ContextArguments.getUser().get(), g, toBeDeleted)) {
            return badRequest("error");
        }
        
        g.members.remove(toBeDeleted);
        g.save();

        return redirect(routes.GroupController.getGroup(groupId));
    }

    public Result getAddMember(int groupId) {
        return ok("..");
    }
}
