package controllers;

import domainlogic.filemanager.FileManager;
import domainlogic.filemanager.FilenameAlreadyExistsException;
import domainlogic.filemanager.QuotaExceededException;
import models.File;
import models.TempFile;
import models.dtos.CreateGroupDto;
import models.dtos.UploadFileDto;
import models.dtos.UploadFileMetaDto;
import models.dtos.UserLoginDto;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import policy.session.Authentication;
import policy.session.SessionManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;

@Singleton
@Authentication.Required
public class FileController extends Controller {
    private final Form<UploadFileMetaDto> uploadFileMetaForm;
    private final SessionManager sessionManager;
    private final FileManager fileManager;

    @Inject
    public FileController(SessionManager sessionManager, FileManager fileManager, FormFactory formFactory) {
        this.uploadFileMetaForm = formFactory.form(UploadFileMetaDto.class);
        this.sessionManager = sessionManager;
        this.fileManager = fileManager;
    }

    public Result changePermissionsForFile()
    {
        return ok("test");
    }

    public Result showUploadFileForm() {
        return ok(views.html.upload.SelectFile.render(null));
    }

    public Result uploadFile() {
        try {
            Http.MultipartFormData<java.io.File> body = request().body().asMultipartFormData();
            Http.MultipartFormData.FilePart<java.io.File> file = body.getFile("file");
            byte[] data = Files.readAllBytes(file.getFile().toPath());
            TempFile tempFile = fileManager.createTempFile(
                sessionManager.currentUser().getUserId(),
                data
            );

            Form<UploadFileMetaDto> boundForm = uploadFileMetaForm.fill(
                    new UploadFileMetaDto(
                            tempFile.getFileId(),
                            file.getFilename(),
                        ""
                    )
            );

            return ok(views.html.upload.FileMeta.render(boundForm));
        }
        catch (QuotaExceededException qe) {
            return badRequest(views.html.upload.SelectFile.render("Quota überschritten!"));
        }
        catch (Exception e) {
            return badRequest(views.html.upload.SelectFile.render(e.getMessage()));
        }
    }

    public Result storeFile() {
        Form<UploadFileMetaDto> boundForm = uploadFileMetaForm.bindFromRequest();
        if (boundForm.hasErrors()) {
            return badRequest(views.html.upload.FileMeta.render(boundForm));
        }

        UploadFileMetaDto formData = boundForm.get();
        try {
            File file = fileManager.storeFile(
                sessionManager.currentUser().getUserId(),
                formData.getTempFileId(),
                formData.getFilename(),
                formData.getComment()
            );

            return ok("File stored. id="+file.getFileId());
        } catch (QuotaExceededException e) {
            boundForm = boundForm.withGlobalError("Quota überschritten!");
            return badRequest(views.html.upload.FileMeta.render(boundForm));
        } catch (FilenameAlreadyExistsException e) {
            boundForm = boundForm.withError("filename", "Ist nicht eindeutig!");
            return badRequest(views.html.upload.FileMeta.render(boundForm));
        }
    }

    public Result listFiles() {
        String files = "";
        for(File f : fileManager.accessibleFiles(sessionManager.currentUser().getUserId())) {
            files += f.getName() + "<br>";
        }

        return ok(files);
    }
}
