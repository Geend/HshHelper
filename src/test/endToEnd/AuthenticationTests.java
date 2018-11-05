package endToEnd;

import com.google.common.collect.ImmutableList;
import extension.test.WSTestClientStateful;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import play.Application;
import play.api.test.CSRFTokenHelper;
import play.libs.ws.WSCookie;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import play.test.TestServer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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
                    { "POST", "/users/create", Http.Status.OK },    // controllers.UserController.createUser
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
                    { "POST", "/users/create", Http.Status.SEE_OTHER },    // controllers.UserController.createUser
                    { "GET", "/users/create", Http.Status.SEE_OTHER },    // controllers.UserController.showCreateUserForm
                    { "GET", "/users", Http.Status.SEE_OTHER },    // controllers.UserController.showUsers
                    { "POST", "/users/delete", Http.Status.SEE_OTHER },    // controllers.UserController.deleteUser
                    { "GET", "/resetpassword", Http.Status.OK },    // controllers.UserController.showResetUserPasswordForm
                    { "POST", "/resetpassword", Http.Status.OK },    // controllers.UserController.resetUserPassword
                    { "GET", "/login", Http.Status.OK },    // controllers.LoginController.showLoginForm
                    { "POST", "/login", Http.Status.BAD_REQUEST },    // controllers.LoginController.login
                    { "POST", "/logout", Http.Status.SEE_OTHER },    // controllers.LoginController.logout
                    { "GET", "/changePasswordAfterReset", Http.Status.OK },    // controllers.LoginController.showChangePasswordAfterResetForm
                    { "POST", "/changePasswordAfterReset", Http.Status.OK },    // controllers.LoginController.changePasswordAfterReset
                    { "GET", "/user/groups", Http.Status.SEE_OTHER },    // controllers.GroupController.showOwnGroups
                    { "GET", "/groups/create", Http.Status.SEE_OTHER },    // controllers.GroupController.showCreateGroupForm
                    { "POST", "/groups/create", Http.Status.SEE_OTHER },    // controllers.GroupController.createGroup
                    { "GET", "/groups/1", Http.Status.SEE_OTHER },    // controllers.GroupController.showGroup(groupId : Long)
                    { "POST", "/groups/1/delete", Http.Status.SEE_OTHER },    // controllers.GroupController.deleteOwnGroup(groupId : Long)
                    { "POST", "/groups/1/members/remove", Http.Status.SEE_OTHER },    // controllers.GroupController.removeGroupMember(groupId : Long)
                    { "POST", "/groups/1/members/add", Http.Status.SEE_OTHER },    // controllers.GroupController.addGroupMember(groupId : Long)
                    { "GET", "/sessions", Http.Status.SEE_OTHER },    // controllers.UserController.showActiveUserSessions()
                    { "POST", "/sessions/delete", Http.Status.SEE_OTHER },    // controllers.UserController.deleteUserSession()
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

