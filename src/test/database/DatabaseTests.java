package database;

import com.google.common.collect.ImmutableMap;
import org.h2.jdbc.JdbcSQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import play.db.Database;
import play.db.Databases;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseTests {

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void verifyThatNoLiteralsAreAllowedInH2DB() throws SQLException {
        Database database = Databases.inMemory(
                "h2-testdb",
                ImmutableMap.of(
                        "ALLOW_LITERALS", "NONE"
                )
        );

        expected.expect(JdbcSQLException.class);
        expected.expectMessage("Literal dieser Art nicht zugelassen");
        Connection connection = database.getConnection();
        connection.prepareStatement("SELECT X FROM SYSTEM_RANGE(1, 10) WHERE X = 1 or '1' = '1'").execute();
    }
}
