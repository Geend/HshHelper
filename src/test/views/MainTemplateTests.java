package views;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.test.Helpers;
import play.twirl.api.Content;
import play.twirl.api.Html;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.contentAsString;

public class MainTemplateTests {

    @Before
    public void setup() {
        Http.Request request = Helpers.fakeRequest().build();
        Http.Context.current.set(Helpers.httpContext(request));
    }

    @Test
    public void renderMainTemplate() {
        String title = "HsH-Helfer";
        Content html = views.html.Main.render(title, new Html("TestContent"));
        assertEquals("text/html", html.contentType());
        assertTrue(contentAsString(html).contains("TestContent"));
        assertTrue(contentAsString(html).contains("<title>" + title + "</title>"));
    }

    @Test
    public void unauthorizedUserDoesNotSeeActionsOnNavBar() {
        String title = "HsH-Helfer";
        Content html = views.html.Main.render(title, new Html("TestContent"));
        assertEquals("text/html", html.contentType());
        assertTrue(contentAsString(html).contains("<nav class=\"navbar navbar-expand-sm navbar-dark bg-hsh-f4-orange\">"));
        assertTrue(contentAsString(html).contains("</nav>"));
        assertFalse(contentAsString(html).contains("<button type=\"submit\" class=\"btn btn-secondary-outline nav-link\">Ausloggen</button>"));
    }

    @Test
    public void authorizedUserCanSeeActionsOnNavBar() {
        // TODO: Untestable like this - I need a logged in user.
        String title = "HsH-Helfer";
        Content html = views.html.Main.render(title, new Html("TestContent"));
        assertEquals("text/html", html.contentType());
        assertEquals(true, contentAsString(html).contains("<a class=\"nav-link\" href=\"/sessions\">"));
        assertEquals(true, contentAsString(html).contains("<a class=\"nav-link\" href=\"/users\">"));
        assertEquals(true, contentAsString(html).contains("<a class=\"nav-link\" href=\"/users/create\">"));
        assertEquals(true, contentAsString(html).contains("<a class=\"nav-link\" href=\"/user/groups\">"));

    }
}
