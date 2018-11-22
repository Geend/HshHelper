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
import org.springframework.util.StringUtils;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;
import play.shaded.ahc.io.netty.util.internal.StringUtil;
import policyenforcement.Policy;
import policyenforcement.session.Authentication;
import policyenforcement.session.SessionManager;
import scala.collection.Seq;

import java.util.*;

import static play.libs.Scala.asScala;

@Singleton
@Authentication.Required
public class PermissionController extends Controller {

    private final PermissionManager manager;
    private final SessionManager sessionManager;

    private final Form<CreateUserPermissionDto> createUserPermissionForm;
    private final Form<CreateGroupPermissionDto> createGroupPermissionForm;

    private final Form<EditGroupPermissionDto> editGroupPermissionForm;
    private final Form<GroupPermissionIdDto> deleteGroupPermissionForm;
    private final Form<EditUserPermissionDto> editUserPermissionForm;
    private final Form<UserPermissionIdDto> deleteUserPermissionForm;
    private final Form<ShowEditUserPermissionFormDto> showEditUserPermissionFormDtoForm;
    private final Form<ShowEditGroupPermissionFormDto> showEditGroupPermissionFormDtoForm;

    @Inject
    public PermissionController(FormFactory formFactory, PermissionManager manager, SessionManager sessionManager, Policy policy) {
        this.createUserPermissionForm = formFactory.form(CreateUserPermissionDto.class);
        this.createGroupPermissionForm = formFactory.form(CreateGroupPermissionDto.class) ;
        this.editGroupPermissionForm = formFactory.form(EditGroupPermissionDto.class);
        this.deleteGroupPermissionForm = formFactory.form(GroupPermissionIdDto.class);
        this.editUserPermissionForm = formFactory.form(EditUserPermissionDto.class);
        this.deleteUserPermissionForm = formFactory.form(UserPermissionIdDto.class);
        this.showEditUserPermissionFormDtoForm = formFactory.form(ShowEditUserPermissionFormDto.class);
        this.showEditGroupPermissionFormDtoForm = formFactory.form(ShowEditGroupPermissionFormDto.class);

        this.manager = manager;
        this.sessionManager = sessionManager;
    }


    /* UserPermissions START */
    public Result showEditUserPermissionForm() throws UnauthorizedException, InvalidArgumentException, InvalidDataException {
        Form<ShowEditUserPermissionFormDto> form = showEditUserPermissionFormDtoForm.bindFromRequest();
        ShowEditUserPermissionFormDto data = form.get();

        EditUserPermissionDto dto = manager.getUserPermissionForEdit(data.getUserPermissionId());
        dto.setReturnUrl(data.getReturnUrl());

        return ok(views.html.filepermissions.EditUserPermission.render(dto, this.editUserPermissionForm.fill(dto)));
    }

    public Result deleteUserPermission() throws UnauthorizedException, InvalidArgumentException {
        Form<UserPermissionIdDto> boundForm = this.deleteUserPermissionForm.bindFromRequest("userPermissionId", "returnUrl");
        if (boundForm.hasErrors()) {
            return badRequest();
        }

        this.manager.deleteUserPermission(boundForm.get().getUserPermissionId());

        if(!StringUtils.isEmpty(boundForm.get().getReturnUrl())) {
            return redirect(boundForm.get().getReturnUrl());
        }

        return redirect(routes.HomeController.index());
    }

    public Result editUserPermission() throws InvalidArgumentException, UnauthorizedException {
        Form<EditUserPermissionDto> boundForm = this.editUserPermissionForm.bindFromRequest("userPermissionId", "permissionLevel", "returnUrl");
        if (boundForm.hasErrors()) {
            return badRequest();
        }
        EditUserPermissionDto dto = boundForm.get();
        this.manager.editUserPermission(dto.getUserPermissionId(), dto.getPermissionLevel());

        if(!StringUtils.isEmpty(dto.getReturnUrl())) {
            return redirect(dto.getReturnUrl());
        }

        return redirect(routes.HomeController.index());
    }
    /* UserPermission END */


    /* GroupPermission START */
    public Result showEditGroupPermissionForm() throws UnauthorizedException, InvalidArgumentException, InvalidDataException {
        Form<ShowEditGroupPermissionFormDto> form = showEditGroupPermissionFormDtoForm.bindFromRequest();
        ShowEditGroupPermissionFormDto data = form.get();

        EditGroupPermissionDto dto = manager.getGroupPermissionForEdit(data.getGroupPermissionId());
        dto.setReturnUrl(data.getReturnUrl());

        return ok(views.html.filepermissions.EditGroupPermission.render(dto, this.editGroupPermissionForm.fill(dto)));
    }

    public Result deleteGroupPermission() throws UnauthorizedException, InvalidArgumentException {
        Form<GroupPermissionIdDto> boundForm = deleteGroupPermissionForm.bindFromRequest("groupPermissionId", "returnUrl");
        if (boundForm.hasErrors()) {
            return badRequest();
        }

        this.manager.deleteGroupPermission(boundForm.get().getGroupPermissionId());

        if(!StringUtils.isEmpty(boundForm.get().getReturnUrl())) {
            return redirect(boundForm.get().getReturnUrl());
        }

        return redirect(routes.HomeController.index());
    }

    public Result editGroupPermission() throws InvalidArgumentException, UnauthorizedException {
        Form<EditGroupPermissionDto> boundForm = this.editGroupPermissionForm.bindFromRequest("groupPermissionId", "permissionLevel", "returnUrl");
        if (boundForm.hasErrors()) {
            return badRequest();
        }

        EditGroupPermissionDto dto = boundForm.get();
        this.manager.editGroupPermission(dto.getGroupPermissionId(), dto.getPermissionLevel());

        if(!StringUtils.isEmpty(dto.getReturnUrl())) {
            return redirect(dto.getReturnUrl());
        }

        return redirect(routes.HomeController.index());
    }
    /* GroupPermission - END */




    /*
        creating permissions
     */

    public Result showCreateGroupPermission()
    {
        return renderCreateGroupPermissionForm(createGroupPermissionForm);
    }

    public Result createGroupPermission() throws UnauthorizedException, InvalidArgumentException {
        Form<CreateGroupPermissionDto> boundForm = createGroupPermissionForm.bindFromRequest("fileId", "groupId", "permissionLevel");

        if (boundForm.hasErrors()) {
            return renderCreateGroupPermissionForm(boundForm);
        }

        CreateGroupPermissionDto createUserPermissionDto = boundForm.get();

        manager.createGroupPermission(createUserPermissionDto.getFileId(), createUserPermissionDto.getGroupId(), createUserPermissionDto.getPermissionLevel());
        return redirect(routes.HomeController.index());
    }

    public Result showCreateUserPermission()
    {
        return renderShowCreateUserPermissionForm(createUserPermissionForm);
    }

    public Result createUserPermission() throws UnauthorizedException, InvalidArgumentException {
        Form<CreateUserPermissionDto> boundForm = createUserPermissionForm.bindFromRequest("fileId", "userId", "permissionLevel");

        if (boundForm.hasErrors()) {
            return renderShowCreateUserPermissionForm(boundForm);
        }

        CreateUserPermissionDto createUserPermissionDto = boundForm.get();

        manager.createUserPermission(createUserPermissionDto.getFileId(), createUserPermissionDto.getUserId(), createUserPermissionDto.getPermissionLevel());
        return redirect(routes.HomeController.index());
    }

    private Result renderCreateGroupPermissionForm(Form<CreateGroupPermissionDto> form){
        List<File> ownFiles = manager.getUserFiles(sessionManager.currentUser().userId);
        List<Group> ownGroups = sessionManager.currentUser().getGroups();
        List<PermissionLevel> possiblePermissions = Arrays.asList(PermissionLevel.values());

        return ok(views.html.filepermissions.CreateGroupPermission.render(form, asScala(ownFiles), asScala(ownGroups), asScala(possiblePermissions)));
    }

    private Result renderShowCreateUserPermissionForm(Form<CreateUserPermissionDto> form){
        List<File> ownFiles = manager.getUserFiles(sessionManager.currentUser().userId);
        List<User> allOtherUsers = manager.getAllOtherUsers(sessionManager.currentUser().userId);
        List<PermissionLevel> possiblePermissions = Arrays.asList(PermissionLevel.values());

        return ok(views.html.filepermissions.CreateUserPermission.render(form, asScala(ownFiles), asScala(allOtherUsers), asScala(possiblePermissions)));
    }

}
