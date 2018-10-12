package controllers;

import models.Group;
import models.dtos.CreateGroupDTO;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GroupController extends Controller {
    private final Form<CreateGroupDTO> groupForm;

    @Inject
    public GroupController(FormFactory formFactory) {
        this.groupForm = formFactory.form(CreateGroupDTO.class);
    }

    public Result getCreateUser() {
        return ok(views.html.createGroup.render(groupForm));
    }

    public Result postCreateUser() {
        Form<CreateGroupDTO> bf = groupForm.bindFromRequest();

        if(bf.hasErrors()) {
            return badRequest(views.html.createGroup.render(bf));
        } else {
            CreateGroupDTO gDto = bf.get();
            Group group = new Group();
            group.name = gDto.getName();
            Group.addGroup(group);
            return redirect("/");
        }
    }
}
