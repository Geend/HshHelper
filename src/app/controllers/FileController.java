package controllers;

import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.filemanager.FileManager;
import managers.filemanager.FilenameAlreadyExistsException;
import managers.filemanager.QuotaExceededException;
import models.File;
import models.TempFile;
import dtos.*;
import models.finders.UserQuota;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import policyenforcement.session.Authentication;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import javax.inject.Singleton;
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

    public Result changePermissionsForFile() {
        return ok("test");
    }

    public Result showUploadFileForm() {
        return ok(views.html.file.upload.SelectFile.render(null));
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

            return ok(views.html.file.upload.FileMeta.render(boundForm));
        } catch (QuotaExceededException qe) {
            return badRequest(views.html.file.upload.SelectFile.render("Quota überschritten!"));
        } catch (Exception e) {
            return badRequest(views.html.file.upload.SelectFile.render(e.getMessage()));
        }
    }

    public Result storeFile() throws UnauthorizedException {
        Form<UploadFileMetaDto> boundForm = uploadFileMetaForm.bindFromRequest();
        if (boundForm.hasErrors()) {
            return badRequest(views.html.file.upload.FileMeta.render(boundForm));
        }

        UploadFileMetaDto formData = boundForm.get();
        try {
            File file = fileManager.storeFile(
                    sessionManager.currentUser().getUserId(),
                    formData.getTempFileId(),
                    formData.getFilename(),
                    formData.getComment()
            );

            return ok("File stored. id=" + file.getFileId());
        } catch (QuotaExceededException e) {
            boundForm = boundForm.withGlobalError("Quota überschritten!");
            return badRequest(views.html.file.upload.FileMeta.render(boundForm));
        } catch (FilenameAlreadyExistsException e) {
            boundForm = boundForm.withError("filename", "Ist nicht eindeutig!");
            return badRequest(views.html.file.upload.FileMeta.render(boundForm));
        }
    }

    public Result listFiles() {
        String files = "";
        for (File f : fileManager.accessibleFiles(sessionManager.currentUser().getUserId())) {
            files += f.getName() + "<br>";
        }

        return ok(files);
    }

    public Result showQuotaUsage() {
        UserQuota uq = fileManager.getCurrentQuotaUsage(sessionManager.currentUser().getUserId());
        UserQuotaDto dto = new UserQuotaDto(
                (long) sessionManager.currentUser().getQuotaLimit(),
                uq.getNameUsage(),
                uq.getCommentUsage(),
                uq.getFileContentUsage(),
                uq.getTempFileContentUsage()
        );

        return ok(views.html.file.UserQuota.render(dto));
    }


    public Result showFile(long fileId) throws UnauthorizedException, InvalidArgumentException {
        File file = fileManager.getFile(sessionManager.currentUser(), fileId);
        boolean isOwner = file.getOwner().equals(sessionManager.currentUser());

        return ok(views.html.file.File.render(file, isOwner, false));
    }

}
