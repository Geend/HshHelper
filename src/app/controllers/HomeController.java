package controllers;

import managers.filemanager.FileManager;
import models.File;
import models.User;
import play.mvc.Controller;
import play.mvc.Result;
import policyenforcement.session.Authentication;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;

import static play.libs.Scala.asScala;

@Singleton
@Authentication.Required
public class HomeController extends Controller {
    private final SessionManager sessionManager;
    private final FileManager fileManager;

    @Inject
    public HomeController(SessionManager sessionManager, FileManager fileManager) {
        this.sessionManager = sessionManager;
        this.fileManager = fileManager;
    }

    public Result index() {
        List<File> accessibleFiles = fileManager.accessibleFiles();
        return ok(views.html.Index.render(asScala(accessibleFiles)));
    }

}