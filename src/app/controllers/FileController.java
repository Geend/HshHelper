package controllers;

import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.filemanager.FileManager;
import managers.filemanager.FilenameAlreadyExistsException;
import managers.filemanager.QuotaExceededException;
import models.File;
import models.TempFile;
import dtos.*;
import models.User;
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
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static play.libs.Scala.asScala;

@Singleton
@Authentication.Required
public class FileController extends Controller {
    private final Form<UploadFileMetaDto> uploadFileMetaForm;

    private final Form<EditFileDto> editFileForm;


    private final SessionManager sessionManager;
    private final FileManager fileManager;

    @Inject
    public FileController(SessionManager sessionManager, FileManager fileManager, FormFactory formFactory) {
        this.uploadFileMetaForm = formFactory.form(UploadFileMetaDto.class);
        this.editFileForm = formFactory.form(EditFileDto.class);
        this.sessionManager = sessionManager;
        this.fileManager = fileManager;
    }


    public Result showOwnFiles() {
        User user = sessionManager.currentUser();
        List<File> files = user.getOwnedFiles();
        return ok(views.html.file.Files.render(asScala(files)));
    }

    public Result showSharedFiles() {
        User user = sessionManager.currentUser();
        // TODO: Durch qry ersetzen!
        List<File> files = user.getOwnedFiles().stream().filter(x -> x.getGroupPermissions().size() > 0 || x.getUserPermissions().size() > 0).collect(Collectors.toList());
        return ok(views.html.file.SharedFiles.render(asScala(files)));
    }

    public Result showThirdPartyFiles() {
        return ok("");
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
                    formData.getTempFileId(),
                    formData.getFilename(),
                    formData.getComment()
            );

            return redirect(routes.FileController.showFile(file.getFileId()));
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
        for (File f : fileManager.accessibleFiles()) {
            files += f.getName() + "<br>";
        }

        return ok(files);
    }

    public Result showQuotaUsage() {
        UserQuota uq = fileManager.getCurrentQuotaUsage();
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
        File file = fileManager.getFile(fileId);
        boolean isOwner = file.getOwner().equals(sessionManager.currentUser());


        EditFileDto editFileDto = new EditFileDto();
        editFileDto.setFileId(fileId);
        editFileDto.setComment(file.getComment());
        Form<EditFileDto> form = editFileForm.fill(editFileDto);


        return ok(views.html.file.File.render(file, form));
    }


    public Result downloadFile(long fileId) throws UnauthorizedException, InvalidArgumentException {
        File file = fileManager.getFile(fileId);
        return ok(file.getData()).as("application/octet-stream").withHeader("Content-Disposition", "attatchment; filename=" + file.getName());
    }

    public Result editFile() throws UnauthorizedException, InvalidArgumentException {

        Form<EditFileDto> boundForm = editFileForm.bindFromRequest("fileId", "comment");
        if (boundForm.hasErrors()) {
            return badRequest();
        }

        EditFileDto editFileDto = boundForm.get();


        Http.MultipartFormData<java.io.File> body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart<java.io.File> file = body.getFile("data");

        try {
            if (file == null) {
                fileManager.editFile(sessionManager.currentUser(), editFileDto.getFileId(), editFileDto.getComment());
            } else {
                try {
                    byte[] data = Files.readAllBytes(file.getFile().toPath());
                    fileManager.editFile(sessionManager.currentUser(), editFileDto.getFileId(), editFileDto.getComment(), data);

                } catch (IOException e) {
                    //TODO
                }

            }


        } catch (QuotaExceededException e) {
            return badRequest("Quota überschritten!");
        }
        return showFile(editFileDto.getFileId());

    }

    public Result removeTempFiles() {

        fileManager.removeTempFiles();

        return redirect(routes.FileController.showQuotaUsage());
    }


}