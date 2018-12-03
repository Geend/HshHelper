package models;

import io.ebean.Finder;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import play.Application;
import play.test.Helpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class UserTests {
    private static Application app;

    @BeforeClass
    public static void startApp() {
        app = Helpers.fakeApplication();
        Helpers.start(app);
    }

    @AfterClass
    public static void stopApp() {
        Helpers.stop(app);
    }

    @Test
    public void DeletingUsersDeletesHisGroups() {
        User user = new User("testUser", "", "", false, 5l);
        Group group1 = new Group("testGroup1", user);
        Group group2 = new Group("testGroup2", user);
        List<Group> groups = new ArrayList<>();
        groups.add(group1);
        groups.add(group2);
        user.setOwnerOf(groups);
        user.save();

        Finder<Long, User> userFinder = new Finder<>(User.class);
        Finder<Long, Group> groupFinder = new Finder<>(Group.class);

        assertNotEquals((long)user.getUserId(), 0);
        assertNotEquals((long)group1.getGroupId(), 0);
        assertNotEquals((long)group1.getGroupId(), 0);

        User foundUser = userFinder.byId(user.getUserId());
        assertNotNull(foundUser);
        Group foundGroup = groupFinder.byId(group1.getGroupId());
        assertNotNull(foundGroup);
        foundGroup = groupFinder.byId(group2.getGroupId());
        assertNotNull(foundGroup);

        user.delete();

        foundUser = userFinder.byId(user.getUserId());
        assertNull(foundUser);
        foundGroup = groupFinder.byId(group1.getGroupId());
        assertNull(foundGroup);
        foundGroup = groupFinder.byId(group2.getGroupId());
        assertNull(foundGroup);
    }

    @Test
    public void DeletingUsersDeletesHisFiles() {
        User user = new User("testUser", "", "", false, 5l);
        user.save();

        File f1 = new File();
        f1.setName("admin.txt");
        f1.setComment("blablabla");
        f1.setOwner(user);
        f1.setData(new byte[]{1,2,1,2});
        f1.setWrittenBy(user);
        f1.setWrittenByDt(DateTime.now());
        f1.save();

        Finder<Long, User> userFinder = new Finder<>(User.class);
        Finder<Long, File> fileFinder = new Finder<>(File.class);

        assertNotEquals((long)user.getUserId(), 0);
        assertNotEquals((long)f1.getFileId(), 0);

        User foundUser = userFinder.byId(user.getUserId());
        assertNotNull(foundUser);
        File foundFile = fileFinder.byId(f1.getFileId());
        assertNotNull(foundFile);
        user.delete();

        foundUser = userFinder.byId(user.getUserId());
        assertNull(foundUser);
        foundFile = fileFinder.byId(f1.getFileId());
        assertNull(foundFile);
    }
}
