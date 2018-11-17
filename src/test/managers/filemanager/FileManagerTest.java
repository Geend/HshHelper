package managers.filemanager;

import extension.HashHelper;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import models.User;
import models.finders.FileFinder;
import models.finders.TempFileFinder;
import models.finders.UserFinder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import play.Application;
import play.test.Helpers;
import policyenforcement.Policy;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class FileManagerTest {
    public static Application app;

    @AfterClass
    public static void stopApp() {
        Helpers.stop(app);
    }

    @BeforeClass
    public static void setupGlobal() {
        app = Helpers.fakeApplication();
        Helpers.start(app);
    }

    @Test
    public void canCreateTempFile() throws QuotaExceededException {
        EbeanServer ebeanServer = mock(EbeanServer.class);
        UserFinder userFinder = mock(UserFinder.class);

        /*
        FileManager fileManager = new FileManager(

        );


        fileManager.createTempFile(
            ansgar.getUserId(),
            new byte[]{1,2,3}
        );*/
    }

    @Test
    public void getCurrentQuotaUsage() {

    }
}