package models.finders;

import models.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import play.Application;
import play.test.Helpers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static org.junit.Assert.assertEquals;


@RunWith(Parameterized.class)
public class UserFinderEmailQueryCaseInsensitiveTests {
    public static Application app;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"test@test.de", true},
                {"TEST@TEST.de", true},
                {"Test@Test.de", true},
                {"notreally@web.de", false},
        });
    }

    private String email;
    private Boolean isPresent;

    public UserFinderEmailQueryCaseInsensitiveTests(String email, Boolean isPresent) {
        this.email = email;
        this.isPresent = isPresent;
    }

    @BeforeClass
    public static void startApp() {
        app = Helpers.fakeApplication();
        Helpers.start(app);
        User user = new User("", "test@test.de", "", false, 5);
        user.save();
    }

    @AfterClass
    public static void stopApp() {
        Helpers.stop(app);
    }

    @Test
    public void CaseInsensitiveTests(){
        UserFinder sut = new UserFinder();
        Optional<User> result = sut.byEmail(this.email, UserFinderQueryOptions.CaseInsensitive);
        assertEquals(result.isPresent(), this.isPresent);
    }
}
