package views;

import models.dtos.UserLoginDto;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import play.api.test.CSRFTokenHelper;
import play.data.Form;
import play.mvc.Http;
import play.test.Helpers;
import play.twirl.api.Content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.contentAsString;

public class LoginTemplateTests {

    @Mock
    Form<UserLoginDto> loginForm;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setup() {
        Http.Request request = CSRFTokenHelper.addCSRFToken(Helpers.fakeRequest()).build();
        Http.Context.current.set(Helpers.httpContext(request));
    }

    @Test
    public void loginTemplateHasCorrectTitle() {
        Boolean captchaRequired = false;

        Content html = views.html.Login.render(loginForm, captchaRequired);
        assertEquals("text/html", html.contentType());
        assertTrue(contentAsString(html).contains("<title>HsH-Helper: Login</title>"));
        assertTrue(contentAsString(html).contains("<form action=\"/login\" method=\"POST\"> "));
        assertTrue(contentAsString(html).contains("<input type=\"hidden\" name=\"csrfToken\""));
        assertTrue(contentAsString(html).contains("<div class=\"form-group\"> "));
        assertTrue(contentAsString(html).contains("<label for=\"username\">Benutzername</label> "));
        assertTrue(contentAsString(html).contains("<br> "));
        assertTrue(contentAsString(html).contains("<input class=\"form-control \" type=\"text\" value=\"\" name=\"username\" id=\"username\" placeholder=\"Benutzernamen hier eingeben\" aria-describedby=\"usernameHelp\"> "));
        assertTrue(contentAsString(html).contains("<small id=\"usernameHelp\" class=\"form-text text-muted\"></small> "));
        assertTrue(contentAsString(html).contains("</div> "));
        assertTrue(contentAsString(html).contains("<div class=\"form-group\"> "));
        assertTrue(contentAsString(html).contains("<label for=\"password\">Passwort</label> "));
        assertTrue(contentAsString(html).contains("<br> "));
        assertTrue(contentAsString(html).contains("<input class=\"form-control \" type=\"password\" value=\"\" name=\"password\" id=\"password\" placeholder=\"Passwort hier eingeben\" aria-describedby=\"passwordHelp\"> "));
        assertTrue(contentAsString(html).contains("<small id=\"passwordHelp\" class=\"form-text text-muted\">Passwort durch Sonderzeichen maskiert</small> "));
        assertTrue(contentAsString(html).contains("</div> "));
        assertTrue(contentAsString(html).contains("<button type=\"submit\" class=\"btn btn-primary\">Login</button> "));
        assertTrue(contentAsString(html).contains("</form>"));
    }

    @Test
    public void renderLoginTemplateWithoutCaptcha() {
        Boolean captchaRequired = false;

        Content html = views.html.Login.render(loginForm, captchaRequired);
        assertEquals("text/html", html.contentType());
    }

    @Test
    public void renderLoginTemplateWithCaptcha() {
        Boolean captchaRequired = true;

        Content html = views.html.Login.render(loginForm, captchaRequired);
        assertEquals("text/html", html.contentType());
    }
}
