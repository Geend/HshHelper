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
    public DatabaseInitialization(Database db) {
        Logger.info("DatabaseInitialization - Prepare DB");

        // TODO: Add new tables for truncation
        // This whole process is super fragile
        // Each new table must be manually added if there has to be data added to the table
        // before the application starts.
        Logger.info("DatabaseInitialization - Prepare DB; Truncate all tables.");
        db.withConnection(connection -> {
            String refIntegrityFalse = "SET REFERENTIAL_INTEGRITY FALSE";
            String refIntegrityTrue = "SET REFERENTIAL_INTEGRITY TRUE";
            String truncateUsers = "TRUNCATE TABLE users";
            String truncateGroups = "TRUNCATE TABLE groups";
            Statement stmt = connection.createStatement();
            stmt.execute(refIntegrityFalse);
            stmt.execute(truncateUsers);
            stmt.execute(truncateGroups);
            stmt.execute(refIntegrityTrue);
        });
        Logger.info("DatabaseInitialization - Prepare DB; Truncated");

        Logger.info("ApplicationStart - Prepare DB; Add new users and groups");
        User u1 = new User("admin", "admin@admin.com", HashHelper.hashPassword("admin"), false, 10);
        User u2 = new User("peter", "peter@gmx.com",  HashHelper.hashPassword("peter"), true, 10);
        User u3 = new User("klaus", "klaus@gmx.com",  HashHelper.hashPassword("klaus"), true, 10);
        User u4 = new User("hans", "hans@gmx.com",  HashHelper.hashPassword("hans"), true, 10);

        Group g1 = new Group("All", u1);
        Group g2 = new Group("Administrators", u1);
        Group g3 = new Group("Peter's Group", u1);

        g1.members = Stream.of(u1, u2, u3, u4).collect(Collectors.toSet());
        g2.members = Stream.of(u1).collect(Collectors.toSet());
        g3.members = Stream.of(u1, u2, u3).collect(Collectors.toSet());

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