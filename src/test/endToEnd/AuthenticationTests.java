package endToEnd;

import com.google.common.collect.ImmutableList;
import extension.test.WSTestClientStateful;
import org.apache.http.message.BasicNameValuePair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import play.Application;
import play.api.test.CSRFTokenHelper;
import play.libs.ws.WSResponse;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import play.test.TestServer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;

@RunWith(Enclosed.class)
public class AuthenticationTests {
    private static TestServer server;
    private static Application app;
    private static WSTestClientStateful ws;

    @BeforeClass
    public static void setup() {
        app = fakeApplication();
        server = new TestServer(19001, app);
        server.start();
        ws = new WSTestClientStateful(19001);
    }

    @AfterClass
    public static void teardown() throws IOException {
        ws.close();
        server.stop();
    }

    public static String getCsrfToken(String url) throws Exception {
        WSResponse response = ws.get(url);
        String body = response.getBody();
        String regex = "csrfToken\" value=\"([^\"]+)";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(body);

        if(!matcher.find()) {
            throw new Exception("Couldn't find any tokens");
        }

        return matcher.group(1);
    }

    public static void login(String username, String password) throws Exception {
        String csrfToken = getCsrfToken("/login");

        WSResponse response = ws.post("/login", ImmutableList.of(
                new BasicNameValuePair("username", username),
                new BasicNameValuePair("password", password),
                new BasicNameValuePair("csrfToken", csrfToken)
        ));
    }

    @RunWith(Parameterized.class)
    public static class AdminUserAuthenticationTests {
        @BeforeClass
        public static void setup() throws Exception {
            login("admin", "admin");
        }

        @Parameterized.Parameters(
                name = "Test #{index} as admin: {0} on endpoint {1} -> HTTP Status {2}"
        )
        public static Collection<Object[]> data() {
            // This list was created with an Regex on our conf/routes file.
            // Regex:        ^(GET|POST)\s*(\/\w*(\/*:*\**\w*)*)\s*(controllers.*)
            // Substituion:  { "\1", "\2", },    // \4
            return Arrays.asList(new Object[][] {
                    // Unauthorized user can only access /login. Accessing another endpoint which
                    // requires authentication, will redirect the unauthorized user to /login.
                    { "POST", "/users/create", Http.Status.BAD_REQUEST },    // controllers.UserController.createUser
                    { "GET", "/users/create", Http.Status.OK },    // controllers.UserController.showCreateUserForm
            });
        }

        // first data value (0) is default
        @Parameterized.Parameter
        public String httpVerb;

        @Parameterized.Parameter(1)
        public String httpEndpoint;

        @Parameterized.Parameter(2)
        public int expectedHttpStatus;

        @Test
        public void checkThatUnauthorizedUserCanOnlyAccessTheirPages() throws Exception {
            if(httpVerb.equals("GET")) {
                WSResponse response = ws.get(httpEndpoint);
                assertEquals(expectedHttpStatus, response.getStatus());
            }

            else if(httpVerb.equals("POST")) {
                String csrfToken = getCsrfToken(httpEndpoint);
                WSResponse response = ws.post(httpEndpoint, ImmutableList.of(
                        new BasicNameValuePair("csrfToken", csrfToken)
                ));
                assertEquals(expectedHttpStatus, response.getStatus());
            }

            else {
                throw new Exception("Unkown method");
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class UnauthorizedUserAuthenticationTests {

        @Parameterized.Parameters(
                name = "Test #{index} as unauthorized user: {0} on endpoint {1} -> HTTP Status {2}"
        )
        public static Collection<Object[]> data() {
            // This list was created with an Regex on our conf/routes file.
            // Regex:        ^(GET|POST)\s*(\/\w*(\/*:*\**\w*)*)\s*(controllers.*)
            // Substituion:  { "\1", "\2", },    // \4
            return Arrays.asList(new Object[][] {
                    // Unauthorized user can only access /login. Accessing another endpoint which
                    // requires authentication, will redirect the unauthorized user to /login.
                    { "GET", "/", Http.Status.SEE_OTHER },    // controllers.HomeController.index

                    { "GET", "/login", Http.Status.OK },    // controllers.LoginController.showLoginForm
                    { "POST", "/login", Http.Status.BAD_REQUEST },    // controllers.LoginController.login
                    { "POST", "/logout", Http.Status.SEE_OTHER },    // controllers.LoginController.logout
                    { "GET", "/changePasswordAfterReset", Http.Status.OK },    // controllers.LoginController.showChangePasswordAfterResetForm
                    { "POST", "/changePasswordAfterReset", Http.Status.BAD_REQUEST },    // controllers.LoginController.changePasswordAfterReset
                    { "GET", "/resetPassword", Http.Status.OK },    // controllers.LoginController.showResetPasswordForm
                    { "POST", "/resetPassword", Http.Status.BAD_REQUEST },    // controllers.LoginController.requestResetPassword
                    { "GET", "/resetPassword/:tokenId", Http.Status.BAD_REQUEST },    // controllers.LoginController.showResetPasswordWithTokenForm(tokenId : java.util.UUID)
                    { "POST", "/resetPassword/:tokenId", Http.Status.BAD_REQUEST },    // controllers.LoginController.resetPasswordWithToken(tokenId : java.util.UUID)

                    { "POST", "/users/create", Http.Status.SEE_OTHER },    // controllers.UserController.createUser
                    { "GET", "/users/create", Http.Status.SEE_OTHER },    // controllers.UserController.showCreateUserForm
                    { "GET", "/users/all", Http.Status.SEE_OTHER },    // controllers.UserController.showUsers
                    { "GET", "/users/admins", Http.Status.SEE_OTHER },    // controllers.UserController.showAdminUsers
                    { "POST", "/users/confirmDelete", Http.Status.SEE_OTHER },    // controllers.UserController.showConfirmDeleteForm
                    { "POST", "/users/delete", Http.Status.SEE_OTHER },    // controllers.UserController.deleteUser
                    { "GET", "/users/1/settings", Http.Status.SEE_OTHER },    // controllers.UserController.showUserAdminSettings(userId:Long)
                    { "POST", "/users/editQuota", Http.Status.SEE_OTHER },    // controllers.UserController.changeUserQuotaLimit
                    { "POST", "/users/deactivateTwoFactorAuth", Http.Status.SEE_OTHER },    // controllers.UserController.deactivateSpecificUserTwoFactorAuth
                    { "GET", "/sessions", Http.Status.SEE_OTHER },    // controllers.UserController.showActiveUserSessions()
                    { "POST", "/sessions/delete", Http.Status.SEE_OTHER },    // controllers.UserController.deleteUserSession()
                    { "GET", "/settings", Http.Status.SEE_OTHER },    // controllers.UserController.showUserSettings()
                    { "POST", "/settings/sessionTimeout", Http.Status.SEE_OTHER },    // controllers.UserController.changeUserSessionTimeout()
                    { "POST", "/settings/changePassword", Http.Status.SEE_OTHER },    // controllers.UserController.changeUserPassword()
                    { "GET", "/settings/confirmActivateTwoFactorAuth", Http.Status.SEE_OTHER },    // controllers.UserController.show2FactorAuthConfirmationForm()
                    { "POST", "/settings/activateTwoFactorAuth", Http.Status.SEE_OTHER },    // controllers.UserController.activateTwoFactorAuth()
                    { "POST", "/settings/deactivateTwoFactorAuth", Http.Status.SEE_OTHER },    // controllers.UserController.deactivateTwoFactorAuth()

                    { "GET", "/groups/membership", Http.Status.SEE_OTHER },    // controllers.GroupController.showOwnMemberships
                    { "GET", "/groups/own", Http.Status.SEE_OTHER },    // controllers.GroupController.showOwnGroups
                    { "GET", "/groups/all", Http.Status.SEE_OTHER },    // controllers.GroupController.showAllGroups
                    { "POST", "/groups/confirmDelete", Http.Status.SEE_OTHER },    // controllers.GroupController.confirmDelete
                    { "POST", "/groups/delete", Http.Status.SEE_OTHER },    // controllers.GroupController.deleteGroup
                    { "GET", "/groups/create", Http.Status.SEE_OTHER },    // controllers.GroupController.showCreateGroupForm
                    { "POST", "/groups/create", Http.Status.SEE_OTHER },    // controllers.GroupController.createGroup
                    { "GET", "/groups/1", Http.Status.SEE_OTHER },    // controllers.GroupController.showGroup(groupId : Long)
                    { "GET", "/groups/1/files", Http.Status.SEE_OTHER },    // controllers.GroupController.showGroupFiles(groupId : Long)
                    { "GET", "/groups/1/members", Http.Status.SEE_OTHER },    // controllers.GroupController.showGroupMembers(groupId : Long)
                    { "POST", "/groups/1/members/remove", Http.Status.SEE_OTHER },    // controllers.GroupController.removeGroupMember(groupId : Long)
                    { "GET", "/groups/1/members/add", Http.Status.SEE_OTHER },    // controllers.GroupController.showAddMemberForm(groupId : Long)
                    { "POST", "/groups/1/members/add", Http.Status.SEE_OTHER },    // controllers.GroupController.addGroupMember(groupId : Long)

                    { "GET", "/files/own", Http.Status.SEE_OTHER },    // controllers.FileController.showOwnFiles
                    { "GET", "/files/shared", Http.Status.SEE_OTHER },    // controllers.FileController.showSharedFiles
                    { "GET", "/files/thirdParty", Http.Status.SEE_OTHER },    // controllers.FileController.showThirdPartyFiles
                    { "POST", "/files/delete", Http.Status.SEE_OTHER },    // controllers.FileController.deleteFile
                    { "GET", "/files/upload", Http.Status.SEE_OTHER },    // controllers.FileController.showUploadFileForm
                    { "POST", "/files/upload", Http.Status.SEE_OTHER },    // controllers.FileController.uploadFile
                    { "GET", "/files/uploadToGroup/1", Http.Status.SEE_OTHER },    // controllers.FileController.showUploadFileToGroupForm(groupId:Long)
                    { "GET", "/files/quota", Http.Status.SEE_OTHER },    // controllers.FileController.showQuotaUsage
                    { "GET", "/files/1", Http.Status.SEE_OTHER },    // controllers.FileController.showFile(fileId: Long)
                    { "GET", "/files/1/download", Http.Status.SEE_OTHER },    // controllers.FileController.downloadFile(fileId: Long)
                    { "POST", "/files/editComment", Http.Status.SEE_OTHER },    // controllers.FileController.editFileComment
                    { "POST", "/files/editContent", Http.Status.SEE_OTHER },    // controllers.FileController.editFileContent
                    { "POST", "/files/search", Http.Status.SEE_OTHER },    // controllers.FileController.searchFiles

                    { "POST", "/permissions/editUserPermission", Http.Status.SEE_OTHER },    // controllers.PermissionController.showEditUserPermissionForm()
                    { "POST", "/permissions/editUserPermission/submit", Http.Status.SEE_OTHER },    // controllers.PermissionController.editUserPermission()
                    { "POST", "/permissions/editGroupPermission", Http.Status.SEE_OTHER },    // controllers.PermissionController.showEditGroupPermissionForm()
                    { "POST", "/permissions/editGroupPermission/submit", Http.Status.SEE_OTHER },    // controllers.PermissionController.editGroupPermission()
                    { "GET", "/permissions/createUserPermission/1", Http.Status.SEE_OTHER },    // controllers.PermissionController.showCreateUserPermission(fileId : Long)
                    { "GET", "/permissions/createGroupPermission/1", Http.Status.SEE_OTHER },    // controllers.PermissionController.showCreateGroupPermission(fileId : Long)
                    { "POST", "/permissions/deletegrouppermission/", Http.Status.SEE_OTHER },    // controllers.PermissionController.deleteGroupPermission()
                    { "POST", "/permissions/deleteuserpermission/", Http.Status.SEE_OTHER },    // controllers.PermissionController.deleteUserPermission()
                    { "POST", "/permissions/creategrouppermission/", Http.Status.SEE_OTHER },    // controllers.PermissionController.createGroupPermission()
                    { "POST", "/permissions/createuserpermission/", Http.Status.SEE_OTHER },    // controllers.PermissionController.createUserPermission()

                    { "GET", "/netservices/all", Http.Status.SEE_OTHER },    // controllers.NetServiceController.showAllNetServices
                    { "GET", "/netservices/create", Http.Status.SEE_OTHER },    // controllers.NetServiceController.showAddNetServiceForm
                    { "POST", "/netservices/create", Http.Status.SEE_OTHER },    // controllers.NetServiceController.createNetService
                    { "POST", "/netservices/confirmDelete", Http.Status.SEE_OTHER },    // controllers.NetServiceController.showDeleteNetServiceConfirmation
                    { "POST", "/netservices/delete", Http.Status.SEE_OTHER },    // controllers.NetServiceController.deleteNetService
                    { "GET", "/netservices/edit/1", Http.Status.SEE_OTHER },    // controllers.NetServiceController.showEditNetService(netServiceId:Long)
                    { "POST", "/netservices/edit", Http.Status.SEE_OTHER },    // controllers.NetServiceController.editNetService
                    { "POST", "/netservices/addparameter", Http.Status.SEE_OTHER },    // controllers.NetServiceController.addNetServiceParameter
                    { "POST", "/netservices/removeparameter", Http.Status.SEE_OTHER },    // controllers.NetServiceController.removeNetServiceParameter
                    { "GET", "/credentials", Http.Status.SEE_OTHER },    // controllers.NetServiceController.showUserNetServiceCredentials
                    { "GET", "/credentials/create", Http.Status.SEE_OTHER },    // controllers.NetServiceController.showCreateNetServiceCredentialForm
                    { "POST", "/credentials/create", Http.Status.SEE_OTHER },    // controllers.NetServiceController.createNetServiceCredential
                    { "POST", "/credentials/delete", Http.Status.SEE_OTHER },    // controllers.NetServiceController.deleteNetServiceCredential
                    { "POST", "/credentials/decrypt", Http.Status.SEE_OTHER },    // controllers.NetServiceController.decryptNetServiceCredential(credentialId : Long)

                    { "GET", "/forbidden", Http.Status.FORBIDDEN },    // controllers.ErrorController.showForbiddenMessage()
                    { "GET", "/badrequest", Http.Status.BAD_REQUEST },    // controllers.ErrorController.showBadRequestMessage()

                    { "GET", "/assets/css/style.css", Http.Status.OK },    // controllers.Assets.at(path="/public", file)
            });
        }

        // first data value (0) is default
        @Parameterized.Parameter
        public String httpVerb;

        @Parameterized.Parameter(1)
        public String httpEndpoint;

        @Parameterized.Parameter(2)
        public int expectedHttpStatus;

        @Test
        public void checkThatUnauthorizedUserCanOnlyAccessTheirPages() {
            Http.RequestBuilder builder = Helpers.fakeRequest(httpVerb, httpEndpoint)
                    .host("localhost:19001");
            builder = CSRFTokenHelper.addCSRFToken(builder);
            Result result = Helpers.route(app, builder);
            assertEquals(expectedHttpStatus, result.status());
        }
    }
}

