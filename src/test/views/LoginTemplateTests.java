package views;

import models.dtos.UserLoginDto;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openqa.selenium.By;
import play.api.test.CSRFTokenHelper;
import play.data.Form;
import play.mvc.Http;
import play.test.Helpers;
import play.test.WithBrowser;
import play.twirl.api.Content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.test.Helpers.contentAsString;

public class LoginTemplateTests extends WithBrowser {

    @Before
    public void setup() {
        browser.goTo("/login");
        assertEquals(browser.url(), "login");
        assertThat(browser.$("title").text()).isNotEmpty();
    }


    @Test
    public void loginTemplateHasCorrectTitle() {
        assertThat(browser.$("title").text()).isNotEmpty();
        assertThat(browser.$("title").text()).isEqualTo("HsH-Helper: Login");
    }

    @Test
    public void loginTemplateHasCorrentForm() {
        assertThat(browser.find(By.xpath("/html/body/div/div/div/main/div/form"))).isNotEmpty();
        assertThat(browser.find(By.xpath("/html/body/div/div/div/main/div/form/input"))).isNotEmpty();
        assertThat(browser.find(By.xpath("//*[@id=\"password\"]"))).isNotEmpty();
        assertThat(browser.find(By.xpath("//*[@id=\"username\"]"))).isNotEmpty();
        assertThat(browser.find(By.xpath("/html/body/div/div/div/main/div/form/button"))).isNotEmpty();
        assertThat(browser.find(By.xpath("/html/body/div/div/div/main/div/form/button")).text())
                .isEqualTo("Login");
        assertThat(browser.find(By.className(".g-recaptcha"))).isEmpty();
    }

    @Test
    public void renderLoginTemplateWithCaptcha() {
        // Falsely login 5 times to trigger the captcha
        for (int i = 0; i < 5; i++) {
            browser.find(By.xpath("//*[@id=\"username\"]")).write("admin");
            browser.find(By.xpath("//*[@id=\"password\"]")).write("admin");
            browser.find(By.xpath("/html/body/div/div/div/main/div/form/button")).click();
        }
        assertThat(browser.find(By.className(".g-recaptcha"))).isNotEmpty();
    }
}
