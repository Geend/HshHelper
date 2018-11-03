package endToEnd;

import com.typesafe.config.ConfigFactory;
import controllers.LoginController;
import controllers.UserController;
import controllers.routes;
import org.apache.http.protocol.HTTP;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import play.Application;
import play.api.http.HttpErrorHandlerExceptions;
import play.api.test.CSRFTokenHelper;
import play.filters.csrf.CSRF;
import play.filters.csrf.CSRFFilter;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.ws.WSClient;
import play.libs.ws.WSCookie;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.EssentialFilter;
import play.mvc.Http;
import play.mvc.Result;
import play.test.Helpers;
import play.test.TestServer;
import play.test.WSTestClient;
import play.test.WithApplication;
import router.Routes;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static io.ebean.Ebean.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.stop;

@RunWith(Enclosed.class)
public class AuthenticationTests {

    private static WSClient ws;
    private static TestServer server;
    private static Application app;

    @BeforeClass
    public static void setup() {
        app = fakeApplication();
        server = new TestServer(19001, app);
        ws = WSTestClient.newClient(19001);
        server.start();
    }

    @AfterClass
    public static void teardown() {
        try {
            ws.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.stop();
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
                    { "GET", "/user/changepassword", Http.Status.SEE_OTHER },    // controllers.UserController.showChangeOwnPasswordForm
                    { "POST", "/user/changepassword", Http.Status.SEE_OTHER },    // controllers.UserController.changeOwnPassword
                    { "GET", "/resetpassword", Http.Status.OK },    // controllers.UserController.showResetUserPasswordForm
                    { "POST", "/resetpassword", Http.Status.OK },    // controllers.UserController.resetUserPassword
                    { "GET", "/login", Http.Status.OK },    // controllers.LoginController.showLoginForm
                    { "POST", "/login", Http.Status.BAD_REQUEST },    // controllers.LoginController.login
                    { "POST", "/logout", Http.Status.SEE_OTHER },    // controllers.LoginController.logout
                    { "GET", "/changePasswordAfterReset", Http.Status.OK },    // controllers.LoginController.showChangePasswordAfterResetForm
                    { "POST", "/changePasswordAfterReset", Http.Status.SEE_OTHER },    // controllers.LoginController.changePasswordAfterReset
                    { "GET", "/user/groups", Http.Status.SEE_OTHER },    // controllers.GroupController.showOwnGroups
                    { "GET", "/groups/create", Http.Status.SEE_OTHER },    // controllers.GroupController.showCreateGroupForm
                    { "POST", "/groups/create", Http.Status.SEE_OTHER },    // controllers.GroupController.createGroup
                    { "GET", "/groups/1", Http.Status.SEE_OTHER },    // controllers.GroupController.showGroup(groupId : Long)
                    { "POST", "/groups/1/delete", Http.Status.SEE_OTHER },    // controllers.GroupController.deleteGroup(groupId : Long)
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

    public static class AdminAuthenticationTests {

        @Test
        public void checkThatAdminCanLogin() {
            HashMap<String, String> formValues = new HashMap<String, String>();
            formValues.put("username", "admin");
            formValues.put("password", "admin");
            Http.RequestBuilder builder = new Http.RequestBuilder()
                    .method("POST")
                    .uri("/login")
                    .bodyForm(formValues);
            builder = CSRFTokenHelper.addCSRFToken(builder);
            Result result = Helpers.route(app, builder);
            assertEquals(Http.Status.SEE_OTHER, result.status());
            Optional<String> location = result.redirectLocation();
            assertThat(location.get()).isEqualTo("/");
        }

        @Test
        public void loginAdminWithWSClient() {
            try {
                CompletionStage<WSResponse> postRequest = ws.url("/login")
                        .addHeader("Csrf-Token", "nocheck")
                        .setContentType("application/x-www-form-urlencoded")
                        .post("username=admin&password=admin");
                WSResponse response = postRequest.toCompletableFuture().get();
                assertEquals(Http.Status.SEE_OTHER, response.getStatus());
                assertEquals(response.getSingleHeader("Location").get(), "/");

                List<WSCookie> cookies = new ArrayList<>();
                cookies.add(response.getCookie("PLAY_SESSION").get());

                CompletionStage<WSResponse> getRequest = ws
                        .url(response.getSingleHeader("Location").get())
                        .setCookies(cookies)
                        .get();
                WSResponse getResponse = getRequest.toCompletableFuture().get();
                assertEquals(Http.Status.OK, getResponse.getStatus());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}

