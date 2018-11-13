import javax.inject.*;

import extension.HashHelper;
import models.*;
import play.Logger;
import play.db.Database;

import java.sql.Statement;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class DatabaseInitialization {

    @Inject
    public DatabaseInitialization(Database db, HashHelper hashHelper) {
        Logger.info("DatabaseInitialization - Prepare DB");

        // TODO: Add new tables for truncation
        // This whole process is super fragile
        // Each new table must be manually added if there has to be data added to the table
        // before the application starts.
        Logger.info("DatabaseInitialization - Prepare DB; Truncate all tables.");
        db.withConnection(connection -> {
            Statement stmt = connection.createStatement();
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.execute("TRUNCATE TABLE groupmembers");
            stmt.execute("TRUNCATE TABLE users");
            stmt.execute("TRUNCATE TABLE groups");
            stmt.execute("TRUNCATE TABLE files");
            stmt.execute("TRUNCATE TABLE group_permissions");
            stmt.execute("TRUNCATE TABLE user_permissions");
            stmt.execute("TRUNCATE TABLE internal_session");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
            stmt.execute("SET ALLOW_LITERALS NONE");
        });
        Logger.info("DatabaseInitialization - Prepare DB; Truncated");

        Logger.info("ApplicationStart - Prepare DB; Add new users and groups");
        User u1 = new User("admin", "hsh.helper+admin@gmail.com", hashHelper.hashPassword("admin"), false, 10);
        User u2 = new User("peter", "hsh.helper+peter@gmail.com",  hashHelper.hashPassword("peter"), false, 10);
        User u3 = new User("klaus", "hsh.helper+klaus@gmail.com",  hashHelper.hashPassword("klaus"), false, 10);
        User u4 = new User("hans", "hsh.helper+hans@gmail.com",  hashHelper.hashPassword("hans"), true, 10);

        Group g1 = new Group("All", u1);
        Group g2 = new Group("Administrators", u1);
        Group g3 = new Group("Peter's Group", u2);

        g1.setIsAllGroup(true);
        g2.setIsAdminGroup(true);

        g1.setMembers(Stream.of(u1, u2, u3, u4).collect(Collectors.toSet()));
        g2.setMembers(Stream.of(u1).collect(Collectors.toSet()));
        g3.setMembers(Stream.of(u1, u2, u3).collect(Collectors.toSet()));

        u1.save();
        u2.save();
        u3.save();
        u4.save();

        g1.save();
        g2.save();
        g3.save();


        File f1 = new File();
        f1.setName("admin.txt");
        f1.setComment("blablabla");
        f1.setOwner(u1);
        f1.setData(new byte[]{1,2,1,2});
        f1.save();

        File f2 = new File();
        f2.setName("peter.txt");
        f2.setComment("blabasdasdlabla");
        f2.setOwner(u2);
        f2.setData(new byte[]{1,2,1,2});
        f2.save();

        File f3 = new File();
        f3.setName("klaus.txt");
        f3.setComment("xaxax");
        f3.setOwner(u1);
        f3.setData(new byte[]{1,2,1,2});
        f3.save();


        UserPermission up1 = new UserPermission();
        up1.setUser(u2);
        up1.setFile(f3);
        up1.setCanRead(true);
        up1.setCanWrite(true);
        up1.save();

        GroupPermission gp1 = new GroupPermission();
        gp1.setGroup(g1);
        gp1.setFile(f1);
        gp1.setCanRead(true);
        gp1.setCanWrite(true);
        gp1.save();


        Logger.info("DatabaseInitialization - Prepare DB; Done adding new users and groups");
    }
}