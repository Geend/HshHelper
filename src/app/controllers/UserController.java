package controllers;

import dtos.group.UserIdDto;
import dtos.user.*;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.loginmanager.CaptchaRequiredException;
import managers.usermanager.*;
import models.User;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import policyenforcement.session.Authentication;
import policyenforcement.session.Session;
import policyenforcement.session.SessionManager;
import twofactorauth.QrCodeUtil;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static extension.StringHelper.empty;
import static play.libs.Scala.asScala;

@Authentication.Required
public class UserController extends Controller {

    private final UserManager userManager;
    private final Form<CreateUserDto> createUserForm;
    private final Form<ResetUserPasswordDto> resetUserPasswordForm;
    private final Form<DeleteSessionDto> deleteSessionForm;
    private final Form<UserIdDto> deleteUserForm;
    private final Form<ChangeUserSessionTimeoutDto> changeUserSessionTimeoutForm;
    private final Form<ChangeOwnPasswordDto> changeOwnPasswordForm;
    private final Form<ChangeUserQuotaLimitDto> changeUserQuotaLimitForm;
    private final Form<TwoFactorAuthDto> twoFactorAuthForm;

    private final SessionManager sessionManager;


    @Inject
    public UserController(FormFactory formFactory, UserManager userManager, SessionManager sessionManager) {
        this.userManager = userManager;
        this.createUserForm = formFactory.form(CreateUserDto.class);
        this.resetUserPasswordForm = formFactory.form(ResetUserPasswordDto.class);
        this.deleteSessionForm = formFactory.form(DeleteSessionDto.class);
        this.deleteUserForm = formFactory.form(UserIdDto.class);
        this.changeUserSessionTimeoutForm = formFactory.form(ChangeUserSessionTimeoutDto.class);
        this.changeOwnPasswordForm = formFactory.form(ChangeOwnPasswordDto.class);
        this.changeUserQuotaLimitForm = formFactory.form(ChangeUserQuotaLimitDto.class);
        this.twoFactorAuthForm = formFactory.form(TwoFactorAuthDto.class);

        this.sessionManager = sessionManager;
    }

    public Result showUsers() throws UnauthorizedException {
        List<User> users = this.userManager.getAllUsers();
        return ok(views.html.users.Users.render(asScala(users)));
    }

    public Result showAdminUsers() throws UnauthorizedException {
        List<User> admins = this.userManager.getAdminUsers();
        return ok(views.html.users.Users.render(asScala(admins)));
    }

    public Result showConfirmDeleteForm() throws UnauthorizedException, InvalidArgumentException {
        Form<UserIdDto> boundForm = this.deleteUserForm.bindFromRequest("userId");
        if (boundForm.hasErrors()) {
            return badRequest();
        }

        Long userToDeleteId = boundForm.get().getUserId();
        UserMetaInfo info = userManager.getUserMetaInfo(userToDeleteId);

        return ok(views.html.users.DeleteUserConfirmation.render(info, userToDeleteId));
    }

    public Result deleteUser() throws UnauthorizedException, InvalidArgumentException {
        Form<UserIdDto> boundForm = this.deleteUserForm.bindFromRequest("userId");
        if (boundForm.hasErrors()) {
            return badRequest();
        }
        Long userToDeleteId = boundForm.get().getUserId();

        this.userManager.deleteUser(userToDeleteId);

        return redirect(routes.UserController.showUsers());
    }

    public Result showCreateUserForm() throws UnauthorizedException {
        if (!sessionManager.currentPolicy().canCreateUser()) {
            throw new UnauthorizedException();
        }

        return ok(views.html.users.CreateUser.render(createUserForm));
    }

    public Result createUser() throws UnauthorizedException, InvalidArgumentException {
        Form<CreateUserDto> boundForm = createUserForm.bindFromRequest("username", "email", "quotaLimit");
        if (boundForm.hasErrors()) {
            return badRequest(views.html.users.CreateUser.render(boundForm));
        }
        CreateUserDto createUserDto = boundForm.get();
        try {
            String plaintextPassword = this.userManager.createUser(
                    createUserDto.getUsername(),
                    createUserDto.getEmail(),
                    createUserDto.getQuotaLimit());

            UserCreatedDto userCreatedDto = new UserCreatedDto();
            userCreatedDto.setUsername(createUserDto.getUsername());
            userCreatedDto.setPlaintextPassword(plaintextPassword);
            return ok(views.html.users.UserCreated.render(userCreatedDto));
        } catch (EmailAlreadyExistsException e) {
            boundForm = boundForm.withError("email", "Email already in use");
            return badRequest(views.html.users.CreateUser.render(boundForm));
        } catch (UsernameAlreadyExistsException e) {
            throw new InvalidArgumentException(e.getMessage());
        } catch (UsernameCannotBeAdmin usernameCannotBeAdmin) {
            boundForm = boundForm.withError("username", "Username must not be admin");
            return badRequest(views.html.users.CreateUser.render(boundForm));
        }
    }

    public Result showActiveUserSessions() {
        User currentUser = sessionManager.currentUser();
        List<Session> activeSessions = sessionManager.sessionsByUser(currentUser);
        return ok(views.html.users.UserSessions.render(asScala(activeSessions), asScala(currentUser.getLoginAttempts())));
    }

    public Result deleteUserSession() throws InvalidArgumentException, UnauthorizedException {
        Form<DeleteSessionDto> bf = deleteSessionForm.bindFromRequest();
        sessionManager.destroySession(bf.get().getSessionId());
        return redirect(routes.UserController.showActiveUserSessions());
    }

    public Result activateTwoFactorAuth() throws IOException {
        Form<TwoFactorAuthDto> boundForm = this.twoFactorAuthForm.bindFromRequest();
        if(boundForm.hasErrors()) {
            String imageSourceData = QrCodeUtil.LoadQrCodeImageDataFromGoogle("HsH-Helper", boundForm.get().getSecret());
            return badRequest(views.html.users.Confirm2FactorAuth.render(imageSourceData, boundForm));
        }

        TwoFactorAuthDto activationData = boundForm.get();

        Integer activationToken = 0;
        if(!empty(activationData.getActivationToken())) {
            activationToken = Integer.parseInt(activationData.getActivationToken());
        }

        try {

            this.userManager.activateTwoFactorAuth(activationData.getSecret(), activationToken);
        } catch (Invalid2FATokenException e) {
            String imageSourceData = QrCodeUtil.LoadQrCodeImageDataFromGoogle("HsH-Helper", boundForm.get().getSecret());
            boundForm = boundForm.withError("activationToken", "Ungültiges Token!");
            return badRequest(views.html.users.Confirm2FactorAuth.render(imageSourceData, boundForm));
        }

        return redirect(routes.HomeController.index());
    }

    public Result deactivateTwoFactorAuth() {
        this.userManager.deactivateTwoFactorAuth();
        return redirect(routes.HomeController.index());
    }

    public Result show2FactorAuthConfirmationForm() throws IOException {
        String secret = this.userManager.generateTwoFactorSecret();
        String imageSourceData = QrCodeUtil.LoadQrCodeImageDataFromGoogle("HsH-Helper", secret);

        TwoFactorAuthDto dto = new TwoFactorAuthDto();
        dto.setSecret(secret);

        return ok(views.html.users.Confirm2FactorAuth.render(imageSourceData, twoFactorAuthForm.fill(dto)));
    }

    public Result showUserSettings() throws IOException {
        User currentUser = this.sessionManager.currentUser();
        ChangeUserSessionTimeoutDto changeUserSessionTimeoutDto = new ChangeUserSessionTimeoutDto();
        changeUserSessionTimeoutDto.setValueInMinutes(sessionManager.currentUser().getSessionTimeoutInMinutes());
        Form<ChangeUserSessionTimeoutDto> filledForm = changeUserSessionTimeoutForm.fill(changeUserSessionTimeoutDto);

        return ok(views.html.users.UserSettings.render(
                filledForm,
                changeOwnPasswordForm,
                empty(currentUser.getTwoFactorAuthSecret())
                ));
    }

    public Result changeUserSessionTimeout() throws UnauthorizedException, InvalidArgumentException {
        Form<ChangeUserSessionTimeoutDto> boundForm = changeUserSessionTimeoutForm.bindFromRequest("valueInMinutes");

        if(boundForm.hasErrors()) {
            return redirect(routes.UserController.showUserSettings());
        }

        userManager.changeUserSessionTimeout(boundForm.get().getValueInMinutes());

        return redirect(routes.UserController.showUserSettings());
    }

    public Result changeUserPassword() throws UnauthorizedException {
        Form<ChangeOwnPasswordDto> boundForm = changeOwnPasswordForm.bindFromRequest();

        if(boundForm.hasErrors()){
            return redirect(routes.UserController.showUserSettings());
        }

        userManager.changeUserPassword(boundForm.get().getCurrentPassword(), boundForm.get().getNewPassword());

        return redirect(routes.UserController.showUserSettings());
    }

    public Result showUserAdminSettings(Long userId) throws UnauthorizedException, InvalidArgumentException {
        ChangeUserQuotaLimitDto userQuotaLimitDto = new ChangeUserQuotaLimitDto();
        userQuotaLimitDto.setUserId(userId);
        userQuotaLimitDto.setNewQuotaLimit(userManager.getUserQuotaLimit(userId));

        Form<ChangeUserQuotaLimitDto> filledForm = changeUserQuotaLimitForm.fill(userQuotaLimitDto);
        return ok(views.html.users.UserAdminSettings.render(filledForm));
    }


    public Result changeUserQuotaLimit() throws UnauthorizedException, InvalidArgumentException {
        Form<ChangeUserQuotaLimitDto> boundForm = changeUserQuotaLimitForm.bindFromRequest();

        if(boundForm.hasErrors()){
            return badRequest(views.html.users.UserAdminSettings.render(boundForm));
        }

        userManager.changeUserQuotaLimit(boundForm.get().getUserId(), boundForm.get().getNewQuotaLimit());
        return ok(views.html.users.UserAdminSettings.render(boundForm));
    }
}
