package controllers;

import domainlogic.filemanager.FileManager;
import domainlogic.filemanager.FilenameAlreadyExistsException;
import domainlogic.filemanager.QuotaExceededException;
import models.File;
import play.mvc.Controller;
import play.mvc.Result;
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

    public Result changePermissionsForFile()
    {
        return ok("test");
    }


    public Result addFile()
    {
        try {
            fileManager.createFile(
                sessionManager.currentUser().getUserId(),
        "test.xxx",
        "comment",
                new byte[]{}
            );
        } catch (QuotaExceededException e) {
            e.printStackTrace();
        } catch (FilenameAlreadyExistsException e) {
            e.printStackTrace();
        }
        return ok("test");
    }

    public Result listFiles() {
        String files = "";
        for(File f : fileManager.accessibleFiles(sessionManager.currentUser().getUserId())) {
            files += f.getName() + "<br>";
        }

        return ok(files);
    }
}
