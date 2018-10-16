package extension;

import javax.inject.*;

import models.Group;
import models.User;
import play.Logger;
import play.db.Database;

import java.sql.Statement;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class ApplicationStart {

    @Inject
    public ApplicationStart(Database db) {
        Logger.info("ApplicationStart - Prepare DB");

        // TODO: Add new tables for truncation
        // This whole process is super fragile
        // Each new table must be manually added if there has to be data added to the table
        // before the application starts.
        Logger.info("ApplicationStart - Prepare DB; Truncate all tables.");
        db.withConnection(connection -> {
            String truncateUsers = "TRUNCATE TABLE users";
            String truncateGroups = "TRUNCATE TABLE groups";
            Statement stmt = connection.createStatement();
            stmt.execute(truncateUsers);
            stmt.execute(truncateGroups);
        });
        Logger.info("ApplicationStart - Prepare DB; Truncated");

        Logger.info("ApplicationStart - Prepare DB; Add new users and groups");
        User u1 = new User("admin", "admin@admin.com", "admin", true, 10);
        User u2 = new User("peter", "peter@gmx.com", "peter", true, 10);
        User u3 = new User("klaus", "klaus@gmx.com", "klaus", true, 10);
        User u4 = new User("hans", "hans@gmx.com", "hans", true, 10);

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
        Logger.info("ApplicationStart - Prepare DB; Done adding new users and groups");
    }
}



//    insert into users (id, user_name, email, password, password_reset_required, quota_limit)
//    values (0, 'admin', 'admin@admin.com', 'admin', true, 10);
//    insert into users (id, user_name, email, password, password_reset_required, quota_limit)
//    values (1, 'peter', 'peter@gmx.com', 'peter', true, 10);
//    insert into users (id, user_name, email, password, password_reset_required, quota_limit)
//    values (2, 'klaus', 'klaus@gmx.com', 'klaus', true, 10);
//    insert into users (id, user_name, email, password, password_reset_required, quota_limit)
//    values (3, 'hans', 'hans@gmx.com', 'hans', true, 10);
//
//    insert into groups (id, name, ownerid, is_admin_group)
//    values (0, 'All', 0, false);
//    insert into groups (id, name, ownerid, is_admin_group)
//    values (1, 'Administrators', 0, true);
//    insert into groups (id, name, ownerid, is_admin_group)
//    values (2, 'Peters Group', 1, false);
//
//    insert into groupmembers (user_id, group_id)
//    values (0, 0);
//    insert into groupmembers (user_id, group_id)
//    values (1, 0);
//    insert into groupmembers (user_id, group_id)
//    values (2, 0);
//    insert into groupmembers (user_id, group_id)
//    values (3, 0);
//
//    insert into groupmembers (user_id, group_id)
//    values (0, 1);
//
//    insert into groupmembers (user_id, group_id)
//    values (0, 2);
//    insert into groupmembers (user_id, group_id)
//    values (1, 2);
//    insert into groupmembers (user_id, group_id)
//    values (2, 2);