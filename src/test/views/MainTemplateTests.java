package views;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import play.mvc.Http;
import play.test.Helpers;
import play.test.WithBrowser;
import play.twirl.api.Content;
import play.twirl.api.Html;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.contentAsString;

public class MainTemplateTests extends WithBrowser {


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
        browser.goTo("/login");
        assertEquals(browser.url(), "login");
        assertThat(browser.$("title").text()).isNotEmpty();
        browser.find(By.xpath("//*[@id=\"username\"]")).write("admin");
        browser.find(By.xpath("//*[@id=\"password\"]")).write("admin");
        browser.find(By.xpath("/html/body/div/div/div/main/div/form/button")).click();
        assertEquals(browser.find(By.xpath("/html/body/nav/div/ul/li[1]/a")).text(), "Gruppenuebersicht");
        assertEquals(browser.find(By.xpath("/html/body/nav/div/ul/li[2]/a")).text(), "Sessions");
        assertEquals(browser.find(By.xpath("/html/body/nav/div/ul/li[3]/a")).text(), "Benutzer anlegen");
        assertEquals(browser.find(By.xpath("/html/body/nav/div/ul/li[4]/a")).text(), "Benutzerübersicht");
    }
}
