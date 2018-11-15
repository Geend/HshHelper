package controllers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import domainlogic.permissionmanager.PermissionManager;
import models.User;
import models.dtos.PermissionEntryDto;
import play.mvc.Controller;
import play.mvc.Result;
import policy.session.Authentication;
import policy.session.SessionManager;
import scala.collection.Seq;

import java.util.List;

import static play.libs.Scala.asScala;

@Singleton
@Authentication.Required
public class PermissionController extends Controller {

    private PermissionManager manager;
    private SessionManager sessionManager;

    @Inject
    public PermissionController(PermissionManager manager, SessionManager sessionManager) {
        this.manager = manager;
        this.sessionManager = sessionManager;
    }

    public Result editGroupPermission(Long groupPermissionId)
    {
        return ok("edit permission");
    }

    public Result editUserPermission(Long userPermissionId)
    {
        return ok("edit user");
    }

    public Result showCreateGroupPermission()
    {
        return ok("create group permission");
    }

    public Result showCreateUserPermission()
    {
        return ok("create user permission");
    }

    public Result listGrantedPermissions()
    {
        User currentUser = sessionManager.currentUser();
        List<PermissionEntryDto> entries = this.manager.GetAllGrantedPermissions(currentUser.getUserId());
        Seq<PermissionEntryDto> scalaEntries = asScala(entries);
        return ok(views.html.PermissionList.render(scalaEntries));
    }
}
