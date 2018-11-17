package controllers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.permissionmanager.InvalidDataException;
import managers.permissionmanager.PermissionManager;
import models.*;
import models.User;
import models.PermissionLevel;
import models.File;
import dtos.*;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;
import policyenforcement.Policy;
import policyenforcement.session.Authentication;
import policyenforcement.session.SessionManager;
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
    private Form<GroupPermissionIdDto> deleteGroupPermissionForm;
    private Form<EditUserPermissionDto> editUserPermissionForm;
    private Form<UserPermissionIdDto> deleteUserPermissionForm;

    @Inject
    public PermissionController(FormFactory formFactory, PermissionManager manager, SessionManager sessionManager, Policy policy) {
        this.createUserPermissionForm = formFactory.form(CreateUserPermissionDto.class);
        this.createGroupPermissionForm = formFactory.form(CreateGroupPermissionDto.class) ;
        this.editGroupPermissionForm = formFactory.form(EditGroupPermissionDto.class);
        this.deleteGroupPermissionForm = formFactory.form(GroupPermissionIdDto.class);
        this.editUserPermissionForm = formFactory.form(EditUserPermissionDto.class);
        this.deleteUserPermissionForm = formFactory.form(UserPermissionIdDto.class);

        this.manager = manager;
        this.sessionManager = sessionManager;
    }

    /*
        edit group permissions
     */

    public Result showEditGroupPermission(Long groupPermissionId) throws InvalidDataException, UnauthorizedException, InvalidArgumentException {
        User currentUser = sessionManager.currentUser();
        EditGroupPermissionDto dto = this.manager.getGroupPermissionForEdit(currentUser.getUserId(), groupPermissionId);
        GroupPermissionIdDto deleteDto = new GroupPermissionIdDto(dto.getGroupPermissionId());
        return ok(views.html.filepermissions.EditGroupPermission.render(dto, this.editGroupPermissionForm.fill(dto), this.deleteGroupPermissionForm.fill(deleteDto)));
    }

    public Result deleteGroupPermission() throws UnauthorizedException, InvalidArgumentException {
        User currentUser = sessionManager.currentUser();
        Form<GroupPermissionIdDto> boundForm = deleteGroupPermissionForm.bindFromRequest("groupPermissionId");
        if (boundForm.hasErrors()) {
            return badRequest();
        }

        this.manager.deleteGroupPermission(currentUser.getUserId(), boundForm.get().getGroupPermissionId());
        return redirect(routes.PermissionController.listGrantedPermissions());
    }

    public Result editGroupPermission() throws InvalidArgumentException, UnauthorizedException {
        User currentUser = sessionManager.currentUser();
        Form<EditGroupPermissionDto> boundForm = this.editGroupPermissionForm.bindFromRequest("groupPermissionId", "permissionLevel");
        if (boundForm.hasErrors()) {
            return badRequest();
        }
        EditGroupPermissionDto dto = boundForm.get();
        this.manager.editGroupPermission(currentUser.getUserId(), dto.getGroupPermissionId(), dto.getPermissionLevel());
        return redirect(routes.PermissionController.listGrantedPermissions());
    }

    /*
        edit user permissions
     */

    public Result showEditUserPermission(Long userPermissionId) throws InvalidDataException, UnauthorizedException, InvalidArgumentException {
        User currentUser = sessionManager.currentUser();
        EditUserPermissionDto dto = this.manager.getUserPermissionForEdit(currentUser.getUserId(), userPermissionId);
        UserPermissionIdDto deleteDto = new UserPermissionIdDto(dto.getUserPermissionId());
        return ok(views.html.filepermissions.EditUserPermission.render(dto, this.editUserPermissionForm.fill(dto), this.deleteUserPermissionForm.fill(deleteDto)));
    }

    public Result deleteUserPermission() throws UnauthorizedException, InvalidArgumentException {
        User currentUser = sessionManager.currentUser();
        Form<UserPermissionIdDto> boundForm = this.deleteUserPermissionForm.bindFromRequest("userPermissionId");
        if (boundForm.hasErrors()) {
            return badRequest();
        }

        this.manager.deleteUserPermission(currentUser.getUserId(), boundForm.get().getUserPermissionId());
        return redirect(routes.PermissionController.listGrantedPermissions());
    }

    public Result editUserPermission() throws InvalidArgumentException, UnauthorizedException {
        User currentUser = sessionManager.currentUser();
        Form<EditUserPermissionDto> boundForm = this.editUserPermissionForm.bindFromRequest("userPermissionId", "permissionLevel");
        if (boundForm.hasErrors()) {
            return badRequest();
        }
        EditUserPermissionDto dto = boundForm.get();
        this.manager.editUserPermission(currentUser.getUserId(), dto.getUserPermissionId(), dto.getPermissionLevel());
        return redirect(routes.PermissionController.listGrantedPermissions());
    }

    /*
        listing all granted permissions
     */

    public Result listGrantedPermissions()
    {
        User currentUser = sessionManager.currentUser();
        List<PermissionEntryDto> entries = this.manager.getAllGrantedPermissions(currentUser.getUserId());
        Seq<PermissionEntryDto> scalaEntries = asScala(entries);
        return ok(views.html.filepermissions.PermissionList.render(scalaEntries));
    }

    /*
        creating permissions
     */

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

    private Result renderCreateGroupPermissionForm(Form<CreateGroupPermissionDto> form){
        List<File> ownFiles = manager.getUserFiles(sessionManager.currentUser().userId);
        Set<Group> ownGroups = sessionManager.currentUser().getGroups();
        List<PermissionLevel> possiblePermissions = Arrays.asList(PermissionLevel.values());

        return ok(views.html.filepermissions.CreateGroupPermission.render(form, asScala(ownFiles), asScala(ownGroups), asScala(possiblePermissions)));
    }

    private Result renderShowCreateUserPermissionForm(Form<CreateUserPermissionDto> form){
        List<File> ownFiles = manager.getUserFiles(sessionManager.currentUser().userId);
        Set<User> allOtherUsers = manager.getAllOtherUsers(sessionManager.currentUser().userId);
        List<PermissionLevel> possiblePermissions = Arrays.asList(PermissionLevel.values());

        return ok(views.html.filepermissions.CreateUserPermission.render(form, asScala(ownFiles), asScala(allOtherUsers), asScala(possiblePermissions)));
    }

}
