package controllers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dtos.permissions.*;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.filemanager.dto.FileMeta;
import managers.filemanager.dto.PermissionMeta;
import managers.permissionmanager.InvalidDataException;
import managers.permissionmanager.PermissionManager;
import models.Group;
import models.PermissionLevel;
import models.User;
import org.springframework.util.StringUtils;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;
import policyenforcement.session.Authentication;
import policyenforcement.session.SessionManager;

import java.util.Arrays;
import java.util.List;

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
    public PermissionController(FormFactory formFactory, PermissionManager manager, SessionManager sessionManager) {
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
            throw new InvalidArgumentException();
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
            throw new InvalidArgumentException();
        }
        EditUserPermissionDto dto = boundForm.get();

        // permission = none -> delete
        if(!dto.getPermissionLevel().equals(PermissionLevel.NONE)) {
            this.manager.editUserPermission(dto.getUserPermissionId(), dto.getPermissionLevel());
        } else {
            this.manager.deleteUserPermission(dto.getUserPermissionId());
        }

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
            throw new InvalidArgumentException();
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
            throw new InvalidArgumentException();
        }

        EditGroupPermissionDto dto = boundForm.get();

        if(!dto.getPermissionLevel().equals(PermissionLevel.NONE)) {
            this.manager.editGroupPermission(dto.getGroupPermissionId(), dto.getPermissionLevel());
        } else {
            this.manager.deleteGroupPermission(dto.getGroupPermissionId());
        }

        if(!StringUtils.isEmpty(dto.getReturnUrl())) {
            return redirect(dto.getReturnUrl());
        }

        return redirect(routes.HomeController.index());
    }
    /* GroupPermission - END */




    /*
        creating permissions
     */

    public Result showCreateGroupPermission(Long fileId) throws UnauthorizedException, InvalidArgumentException {
        FileMeta file = manager.getFileMeta(fileId);
        List<Group> ownGroups = sessionManager.currentUser().getGroups();
        // Gruppen mit bestehenden Berechtigungen filtern!
        ownGroups.removeIf(
                x -> file.getPermissions().stream().anyMatch(
                        y -> y.getRefId().equals(x.getGroupId()) && y.getType().equals(PermissionMeta.EType.GROUP)
                )
        );
        List<PermissionLevel> possiblePermissions = Arrays.asList(PermissionLevel.values());

        return ok(views.html.filepermissions.CreateGroupPermission.render(createGroupPermissionForm, asScala(ownGroups), asScala(possiblePermissions), file));
    }

    public Result showCreateUserPermission(Long fileId) throws UnauthorizedException, InvalidArgumentException {
        FileMeta file = manager.getFileMeta(fileId);
        List<User> allOtherUsers = manager.getAllOtherUsers(sessionManager.currentUser().userId);
        // Benutzer mit bestehenden berechtigungen filtern!
        allOtherUsers.removeIf(
                x -> file.getPermissions().stream().anyMatch(
                        y -> y.getRefId().equals(x.getUserId()) && y.getType().equals(PermissionMeta.EType.USER)
                )
        );
        List<PermissionLevel> possiblePermissions = Arrays.asList(PermissionLevel.values());

        return ok(views.html.filepermissions.CreateUserPermission.render(createUserPermissionForm, asScala(allOtherUsers), asScala(possiblePermissions), file));
    }


    public Result createGroupPermission() throws UnauthorizedException, InvalidArgumentException {
        Form<CreateGroupPermissionDto> boundForm = createGroupPermissionForm.bindFromRequest("fileId", "groupId", "permissionLevel");
        if (boundForm.hasErrors()) {
            throw new InvalidArgumentException();
        }

        CreateGroupPermissionDto createUserPermissionDto = boundForm.get();

        // None-Permission -> implicit abort
        if(!createUserPermissionDto.getPermissionLevel().equals(PermissionLevel.NONE)) {
            manager.createGroupPermission(createUserPermissionDto.getFileId(), createUserPermissionDto.getGroupId(), createUserPermissionDto.getPermissionLevel());
        }

        return redirect(routes.FileController.showFile(createUserPermissionDto.getFileId()));
    }


    public Result createUserPermission() throws UnauthorizedException, InvalidArgumentException {
        Form<CreateUserPermissionDto> boundForm = createUserPermissionForm.bindFromRequest("fileId", "userId", "permissionLevel");
        if (boundForm.hasErrors()) {
            throw new InvalidArgumentException();
        }

        CreateUserPermissionDto createUserPermissionDto = boundForm.get();

        // None-Permission -> implicit abort
        if(!createUserPermissionDto.getPermissionLevel().equals(PermissionLevel.NONE)) {
            manager.createUserPermission(createUserPermissionDto.getFileId(), createUserPermissionDto.getUserId(), createUserPermissionDto.getPermissionLevel());
        }

        return redirect(routes.FileController.showFile(createUserPermissionDto.getFileId()));
    }
}
