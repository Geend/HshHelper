package controllers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import domainlogic.permissionmanager.PermissionManager;
import models.dtos.PermissionEntryDto;
import play.mvc.Controller;
import play.mvc.Result;
import policy.session.Authentication;
import scala.collection.Seq;

import java.util.ArrayList;

import static play.libs.Scala.asScala;

@Singleton
@Authentication.Required
public class PermissionController extends Controller {

    private PermissionManager manager;

    @Inject
    public PermissionController(PermissionManager manager) {
        this.manager = manager;
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
        ArrayList<PermissionEntryDto> entries = new ArrayList<>();
        entries.add(new PermissionEntryDto(0, "group", "Klaus gruppe", "read", true));
        entries.add(new PermissionEntryDto(1, "user", "Klaus", "write", true));
        Seq<PermissionEntryDto> scalaEntries = asScala(entries);
        return ok(views.html.PermissionList.render(scalaEntries));
    }
}
