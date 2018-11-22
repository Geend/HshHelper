package controllers;

import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.filemanager.FileManager;
import managers.filemanager.QuotaExceededException;
import models.File;
import dtos.*;
import models.GroupPermission;
import models.PermissionLevel;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static play.libs.Scala.asScala;

@Singleton
@Authentication.Required
public class FileController extends Controller {
    private final Form<UploadFileMetaDto> uploadFileMetaForm;
    private final Form<UploadFileDto> uploadFileForm;
    private final Form<DeleteFileDto> deleteFileForm;
    private final Form<EditFileDto> editFileForm;
    private final Form<SearchQueryDto> searchFileForm;


    private final SessionManager sessionManager;
    private final FileManager fileManager;

    @Inject
    public FileController(SessionManager sessionManager, FileManager fileManager, FormFactory formFactory) {
        this.uploadFileForm = formFactory.form(UploadFileDto.class);
        this.uploadFileMetaForm = formFactory.form(UploadFileMetaDto.class);
        this.editFileForm = formFactory.form(EditFileDto.class);
        this.deleteFileForm = formFactory.form(DeleteFileDto.class);
        this.searchFileForm = formFactory.form(SearchQueryDto.class);
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
        List<File> files = fileManager.sharedWithCurrentUserFiles();
        return ok(views.html.file.Files.render(asScala(files)));
    }

    public Result deleteFile() throws UnauthorizedException, InvalidArgumentException {
        Form<DeleteFileDto> boundForm = deleteFileForm.bindFromRequest();
        if (boundForm.hasErrors()) {
            return badRequest();
        }

        fileManager.deleteFile(boundForm.get().getFileId());

        return redirect(routes.FileController.showOwnFiles());
    }

    public Result showUploadFileForm() {
        List<UserPermissionDto> userPermissionDtos = this.fileManager.getUserPermissionDtosForCreate();
        List<GroupPermissionDto> groupPermissionDtos = this.fileManager.getGroupPermissionDtosForCreate();
        return ok(views.html.file.UploadFile.render(this.uploadFileForm, asScala(userPermissionDtos), asScala(groupPermissionDtos)));
    }

    public Result uploadFile() {
        try {
            Form<UploadFileDto> boundForm = uploadFileForm.bindFromRequest();
            if(boundForm.hasErrors()) {
                List<UserPermissionDto> userPermissionDtos = this.fileManager.getUserPermissionDtosForCreate();
                List<GroupPermissionDto> groupPermissionDtos = this.fileManager.getGroupPermissionDtosForCreate();
                return ok(views.html.file.UploadFile.render(boundForm, asScala(userPermissionDtos), asScala(groupPermissionDtos)));
            }

            ArrayList<GroupPermissionDto> groupPermissions = new ArrayList<>();
            ArrayList<UserPermissionDto> userPermissions = new ArrayList<>();
            Map<String, String> fields = boundForm.rawData();
            for (Map.Entry<String, String> entry: fields.entrySet()) {
                PermissionLevel pl = permissionLevelFromFormString(entry.getValue());
                if(pl != PermissionLevel.NONE) {
                    if(entry.getKey().startsWith("user_")) {
                        String userIdString = entry.getKey().substring(5);
                        Long groupId = Long.parseLong(userIdString);
                        userPermissions.add(new UserPermissionDto(groupId, "", pl));
                    }
                    else if(entry.getKey().startsWith("group_")) {
                        String groupIdString = entry.getKey().substring(6);
                        Long groupId = Long.parseLong(groupIdString);
                        groupPermissions.add(new GroupPermissionDto(groupId, "", pl));
                    }
                }
            }

            Http.MultipartFormData<java.io.File> body = request().body().asMultipartFormData();
            Http.MultipartFormData.FilePart<java.io.File> file = body.getFile("file");
            byte[] data = Files.readAllBytes(file.getFile().toPath());
            UploadFileDto uploadFileDto = boundForm.get();
            this.fileManager.createFile(uploadFileDto.getFilename(), uploadFileDto.getComment(), data, userPermissions, groupPermissions);

            return redirect(routes.FileController.showOwnFiles());
        } catch (Exception e) {
            return redirect(routes.ErrorController.showBadRequestMessage());
        }
    }

    private PermissionLevel permissionLevelFromFormString(String s) {
        switch (s) {
            case "Read":
                return PermissionLevel.READ;
            case "Write":
                return PermissionLevel.WRITE;
            case "ReadAndWrite":
                return PermissionLevel.READWRITE;
                default:
            return PermissionLevel.NONE;
        }
    }

    public Result showQuotaUsage() {
        UserQuota uq = fileManager.getCurrentQuotaUsage();
        UserQuotaDto dto = new UserQuotaDto(
                (long) sessionManager.currentUser().getQuotaLimit(),
                uq.getNameUsage(),
                uq.getCommentUsage(),
                uq.getFileContentUsage());

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
        return ok(file.getData()).as("application/octet-stream").withHeader("Content-Disposition", "attachment; filename=" + file.getName());
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
                fileManager.editFile(editFileDto.getFileId(), editFileDto.getComment());
            } else {
                try {
                    byte[] data = Files.readAllBytes(file.getFile().toPath());
                    fileManager.editFile(editFileDto.getFileId(), editFileDto.getComment(), data);

                } catch (IOException e) {
                    //TODO
                }

            }


        } catch (QuotaExceededException e) {
            return badRequest("Quota überschritten!");
        }
        return showFile(editFileDto.getFileId());

    }

    public Result searchFiles(){

        Form<SearchQueryDto> boundForm = searchFileForm.bindFromRequest("query");

        if(boundForm.hasErrors()){

        }

        String query = boundForm.get().getQuery();

        List<File> files = fileManager.searchFile(query);
        return ok(views.html.file.SearchResult.render(asScala(files), boundForm));
    }

}