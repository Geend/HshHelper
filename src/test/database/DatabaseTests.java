package database;

import org.h2.jdbc.JdbcSQLException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import play.db.Database;
import play.db.Databases;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseTests {

    @Rule
    public ExpectedException expected = ExpectedException.none();

    Database database;

    @Before
    public void setupDatabase() {
        database = Databases.inMemory(
                "h2-dbtest"
        );
    }

    @After
    public void shutdownDatabase() {
        database.shutdown();
    }

    @Test
    public void verifyThatNoLiteralsAreAllowedInH2DB() throws SQLException {
        expected.expect(JdbcSQLException.class);
        expected.expectMessage("Literal");
        Connection connection = database.getConnection();
        Statement stmt = connection.createStatement();
        stmt.execute("SET ALLOW_LITERALS NONE");
        stmt.execute("SELECT X FROM SYSTEM_RANGE(1, 10) WHERE X = 1 or '1' = '1'");
    }

    // TODO: Add an test which verifies that the same still holds for our application
    // Problem with that test - how to access that database from the outside?
}
