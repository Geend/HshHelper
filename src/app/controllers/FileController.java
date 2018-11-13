package controllers;

import domainlogic.filemanager.FileManager;
import play.mvc.Controller;
import policy.session.Authentication;
import policy.session.SessionManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Authentication.Required
public class FileController extends Controller {
    private final SessionManager sessionManager;
    private final FileManager fileManager;

    @Inject
    public FileController(SessionManager sessionManager, FileManager fileManager) {
        this.sessionManager = sessionManager;
        this.fileManager = fileManager;
    }

}
