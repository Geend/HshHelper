package controllers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import domainlogic.InvalidArgumentException;
import domainlogic.UnauthorizedException;
import domainlogic.permissionmanager.InvalidDataException;
import domainlogic.permissionmanager.PermissionManager;
import models.*;
import models.User;
import models.PermissionLevel;
import models.File;
import models.dtos.*;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;
import policy.Specification;
import policy.session.Authentication;
import policy.session.SessionManager;
import scala.collection.Seq;

import java.util.*;

import static play.libs.Scala.asScala;

@Singleton
@Authentication.Required
public class PermissionController extends Controller {

    private PermissionManager manager;
    private SessionManager sessionManager;

    private Form<CreateUserPermissionDto> createUserPermissionForm;
    private Form<CreateGroupPermissionDto> createGroupPermissionForm;

    private Form<EditGroupPermissionDto> editGroupPermissionForm;
    private Form<GroupPermissionDto> deleteGroupPermissionForm;

    @Inject
    public PermissionController(FormFactory formFactory, PermissionManager manager, SessionManager sessionManager, Specification specification) {
        this.createUserPermissionForm = formFactory.form(CreateUserPermissionDto.class);
        this.createGroupPermissionForm = formFactory.form(CreateGroupPermissionDto.class) ;
        this.editGroupPermissionForm = formFactory.form(EditGroupPermissionDto.class);
        this.deleteGroupPermissionForm = formFactory.form(GroupPermissionDto.class);

        this.manager = manager;
        this.sessionManager = sessionManager;
    }

    public Result showEditGroupPermission(Long groupPermissionId) throws InvalidDataException {
        User currentUser = sessionManager.currentUser();
        EditGroupPermissionDto dto = this.manager.getGroupPermissionForEdit(currentUser.getUserId(), groupPermissionId);
        GroupPermissionDto deleteDto = new GroupPermissionDto(dto.getGroupPermissionId());
        return ok(views.html.EditGroupPermission.render(this.editGroupPermissionForm.fill(dto), this.deleteGroupPermissionForm.fill(deleteDto)));
    }

    public Result deleteGroupPermission() {
        User currentUser = sessionManager.currentUser();

        Form<GroupPermissionDto> boundForm = deleteGroupPermissionForm.bindFromRequest("groupPermissionId");
        if (boundForm.hasErrors()) {
            return badRequest();
        }

        this.manager.deleteGroupPermission(currentUser.getUserId(), boundForm.get().getGroupPermissionId());
        return redirect(routes.PermissionController.listGrantedPermissions());
    }

    public Result editGroupPermission()
    {
        return ok("edit permission");
    }

    public Result showEditUserPermission(Long userPermissionId)
    {
        return ok("edit user");
    }

    public Result showCreateGroupPermission()
    {
        return renderCreateGroupPermissionForm(createGroupPermissionForm);
    }

    public Result createGroupPermission() throws UnauthorizedException, InvalidArgumentException {
        User currentUser = sessionManager.currentUser();
        Form<CreateGroupPermissionDto> boundForm = createGroupPermissionForm.bindFromRequest("fileId", "groupId", "permissionLevel");

        if (boundForm.hasErrors()) {
            return renderCreateGroupPermissionForm(boundForm);
        }

        CreateGroupPermissionDto createUserPermissionDto = boundForm.get();

        manager.createGroupPermission(currentUser, createUserPermissionDto.getFileId(), createUserPermissionDto.getGroupId(), createUserPermissionDto.getPermissionLevel());
        return redirect(routes.PermissionController.listGrantedPermissions());
    }

    private Result renderCreateGroupPermissionForm(Form<CreateGroupPermissionDto> form){
        List<File> ownFiles = manager.getUserFiles(sessionManager.currentUser().userId);
        Set<Group> ownGroups = sessionManager.currentUser().getGroups();
        List<PermissionLevel> possiblePermissions = Arrays.asList(PermissionLevel.values());

        return ok(views.html.CreateGroupPermission.render(form, asScala(ownFiles), asScala(ownGroups), asScala(possiblePermissions)));
    }

    public Result showCreateUserPermission()
    {
        return renderShowCreateUserPermissionForm(createUserPermissionForm);
    }

    public Result createUserPermission() throws UnauthorizedException, InvalidArgumentException {
        User currentUser = sessionManager.currentUser();
        Form<CreateUserPermissionDto> boundForm = createUserPermissionForm.bindFromRequest("fileId", "userId", "permissionLevel");

        if (boundForm.hasErrors()) {
            return renderShowCreateUserPermissionForm(boundForm);
        }

        CreateUserPermissionDto createUserPermissionDto = boundForm.get();

        manager.createUserPermission(currentUser, createUserPermissionDto.getFileId(), createUserPermissionDto.getUserId(), createUserPermissionDto.getPermissionLevel());
        return redirect(routes.PermissionController.listGrantedPermissions());
    }

    private Result renderShowCreateUserPermissionForm(Form<CreateUserPermissionDto> form){
        List<File> ownFiles = manager.getUserFiles(sessionManager.currentUser().userId);
        Set<User> allOtherUsers = manager.getAllOtherUsers(sessionManager.currentUser().userId);
        List<PermissionLevel> possiblePermissions = Arrays.asList(PermissionLevel.values());

        return ok(views.html.CreateUserPermission.render(form, asScala(ownFiles), asScala(allOtherUsers), asScala(possiblePermissions)));
    }

    public Result listGrantedPermissions()
    {
        User currentUser = sessionManager.currentUser();
        List<PermissionEntryDto> entries = this.manager.getAllGrantedPermissions(currentUser.getUserId());
        Seq<PermissionEntryDto> scalaEntries = asScala(entries);
        return ok(views.html.PermissionList.render(scalaEntries));
    }
}
