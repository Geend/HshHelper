import javax.inject.*;

import extension.HashHelper;
import models.Group;
import models.User;
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
        Logger.info("DatabaseInitialization - Prepare DB; Done adding new users and groups");
    }
}