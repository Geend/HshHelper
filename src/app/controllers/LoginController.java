package controllers;

import dtos.login.ChangePasswordAfterResetDto;
import dtos.login.RequestResetPasswordDto;
import dtos.login.ResetPasswordDto;
import dtos.login.UserLoginDto;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.WeakPasswordException;
import managers.loginmanager.CaptchaRequiredException;
import managers.loginmanager.InvalidLoginException;
import managers.loginmanager.LoginManager;
import managers.loginmanager.PasswordChangeRequiredException;
import play.data.Form;
import play.data.FormFactory;
import play.filters.csrf.CSRF;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import policyenforcement.session.Authentication;

import javax.inject.Inject;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.UUID;

import static extension.StringHelper.empty;

public class LoginController extends Controller {

    private final Form<UserLoginDto> loginForm;
    private final Form<ChangePasswordAfterResetDto> changePasswordForm;
    private final Form<RequestResetPasswordDto> requestResetPasswordForm;
    private final Form<ResetPasswordDto> resetPasswordForm;
    private final LoginManager loginManager;

    @Inject
    public LoginController(
            FormFactory formFactory, LoginManager loginManager) {
        this.loginForm = formFactory.form(UserLoginDto.class);
        this.changePasswordForm = formFactory.form(ChangePasswordAfterResetDto.class);
        this.requestResetPasswordForm = formFactory.form(RequestResetPasswordDto.class);
        this.resetPasswordForm = formFactory.form(ResetPasswordDto.class);
        this.loginManager = loginManager;
    }

    // Not allowed wichtig, kann sonst zum DOS verwendet werden!
    @Authentication.NotAllowed
    public Result showLoginForm() {
        return ok(views.html.login.Login.render(loginForm, false));
    }

    @Authentication.NotAllowed
    public Result login() throws IOException {
        Form<UserLoginDto> boundForm = this.loginForm.bindFromRequest("username", "password", "twofactorpin");
        if (boundForm.hasErrors()) {
            boolean hasRecaptcha = boundForm.rawData().containsKey("g-recaptcha-response");
            return badRequest(views.html.login.Login.render(boundForm, hasRecaptcha));
        }

        Optional<CSRF.Token> token = CSRF.getToken(request());
        if (!token.isPresent()) {
            // bewusst nicht auf bad request sondern wieder auf login da bad request
            // einen angemeldenten benutzer erwartet
            return redirect(routes.LoginController.login());
        }

        UserLoginDto loginData = boundForm.get();
        Optional<String> recaptchaData = boundForm.field("g-recaptcha-response").getValue();
        if (recaptchaData.isPresent()) {
            loginData.setRecaptcha(recaptchaData.get());
        }

        try {
            loginManager.login(
                    loginData.getUsername(),
                    loginData.getPassword(),
                    loginData.getRecaptcha(),
                    Http.Context.current().request(),
                    loginData.getTwofactorpin());
        } catch (CaptchaRequiredException e) {
            boundForm = boundForm.withGlobalError("Löse das reCAPTCHA!");
            return badRequest(views.html.login.Login.render(boundForm, true));
        } catch (InvalidLoginException e) {
            boundForm = boundForm.withGlobalError("Ungültige Anmeldedaten!");
            return badRequest(views.html.login.Login.render(boundForm, e.isCaptchaRequired()));
        } catch (PasswordChangeRequiredException e) {
            boolean hasRecaptcha = boundForm.rawData().containsKey("g-recaptcha-response");
            ChangePasswordAfterResetDto dto = new ChangePasswordAfterResetDto();
            dto.setUsername(loginData.getUsername());
            dto.setCurrentPassword(loginData.getPassword());
            return ok(views.html.login.ChangePasswordAfterReset.render(this.changePasswordForm.fill(dto), hasRecaptcha));
        } catch (GeneralSecurityException e) {
            return badRequest(views.html.login.Login.render(boundForm, false));
        }

        return redirect(routes.HomeController.index());
    }

    // NotAllowed wichtig, kann sonst für DOS verwendet werden!
    @Authentication.NotAllowed
    public Result showChangePasswordAfterResetForm() {
        return ok(views.html.login.ChangePasswordAfterReset.render(this.changePasswordForm, false));
    }

    @Authentication.NotAllowed
    public Result changePasswordAfterReset() throws IOException {
        Form<ChangePasswordAfterResetDto> boundForm = this.changePasswordForm.bindFromRequest("username", "currentPassword", "password", "passwordRepeat");
        if (boundForm.hasErrors()) {
            boolean hasRecaptcha = boundForm.rawData().containsKey("g-recaptcha-response");
            return badRequest(views.html.login.ChangePasswordAfterReset.render(boundForm, hasRecaptcha));
        }

        ChangePasswordAfterResetDto changePasswordData = boundForm.get();
        Optional<String> recaptchaData = boundForm.field("g-recaptcha-response").getValue();
        if (recaptchaData.isPresent()) {
            changePasswordData.setRecaptcha(recaptchaData.get());
        }

        try {
            loginManager.changePassword(
                    changePasswordData.getUsername(),
                    changePasswordData.getCurrentPassword(),
                    changePasswordData.getPassword(),
                    changePasswordData.getRecaptcha(),
                    Http.Context.current().request(),
                    "");
        } catch (InvalidLoginException e) {
            boundForm = boundForm.withGlobalError("Ungültige Anmeldedaten!");
            return badRequest(views.html.login.ChangePasswordAfterReset.render(boundForm, false));
        } catch (CaptchaRequiredException e) {
            boundForm = boundForm.withGlobalError("Löse das reCAPTCHA!");
            return badRequest(views.html.login.ChangePasswordAfterReset.render(boundForm, true));
        } catch (GeneralSecurityException e) {
            return badRequest(views.html.login.ChangePasswordAfterReset.render(boundForm, false));
        } catch (WeakPasswordException e) {
            boundForm = boundForm.withError("password", "Das Passwort ist zu schwach!");
            return badRequest(views.html.login.ChangePasswordAfterReset.render(boundForm, false));
        }

        return redirect(routes.LoginController.login());
    }

    @Authentication.NotAllowed
    public Result showResetPasswordForm() {
        return ok(views.html.login.RequestResetPassword.render(requestResetPasswordForm));
    }

    @Authentication.NotAllowed
    public Result requestResetPassword() {
        Form<RequestResetPasswordDto> boundForm = requestResetPasswordForm.bindFromRequest("username");
        if (boundForm.hasErrors()) {
            return badRequest(views.html.login.RequestResetPassword.render(boundForm));
        }

        RequestResetPasswordDto resetPasswordData = boundForm.get();
        Optional<String> recaptchaData = boundForm.field("g-recaptcha-response").getValue();
        recaptchaData.ifPresent(resetPasswordData::setRecaptcha);

        try {
            this.loginManager.sendResetPasswordToken(resetPasswordData.getUsername(), resetPasswordData.getRecaptcha(), Http.Context.current().request());
        } catch (UnauthorizedException | InvalidArgumentException e) {
            //Ignore the exception in order to not reveal potential usernames.
        } catch (CaptchaRequiredException e) {
            boundForm = boundForm.withGlobalError("Complete the captcha!");
            return badRequest(views.html.login.RequestResetPassword.render(boundForm));
        }

        return ok(views.html.login.RequestResetPasswordSuccess.render());
    }

    @Authentication.NotAllowed
    public Result showResetPasswordWithTokenForm(UUID tokenId) throws UnauthorizedException {
        this.loginManager.validateResetToken(tokenId, Http.Context.current().request());
        return ok(views.html.login.ResetPassword.render(resetPasswordForm, tokenId));
    }

    @Authentication.NotAllowed
    public Result resetPasswordWithToken(UUID tokenId) throws GeneralSecurityException {
        Form<ResetPasswordDto> boundForm = resetPasswordForm.bindFromRequest();
        if (boundForm.hasErrors()) {
            return badRequest(views.html.login.ResetPassword.render(resetPasswordForm, tokenId));
        }

        ResetPasswordDto data = boundForm.get();


        try {
            this.loginManager.resetPassword(tokenId, data.getNewPassword(), Http.Context.current().request(), data.getTwoFactorPin());
        } catch (WeakPasswordException e) {
            boundForm = boundForm.withError("newPassword", "Das Passwort ist zu schwach!");
            return badRequest(views.html.login.ResetPassword.render(boundForm, tokenId));

        } catch (UnauthorizedException | InvalidLoginException e) {
            boundForm = boundForm.withError("twoFactorPin", "Falscher 2FA Pin");
            return badRequest(views.html.login.ResetPassword.render(boundForm, tokenId));
        }

        return redirect(routes.LoginController.login());
    }


    @Authentication.Required
    public Result logout() {
        loginManager.logout();
        return redirect(routes.LoginController.login());
    }
}
