import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * KLasse um zur Datenbank zu connecten
 */
public class Verbindung
{

        private static final String url = "jdbc:postgresql://localhost:5433/Testat2";
        private static final String user = "postgres";
        private static final String password = "postgres";

        public static Connection connect() throws SQLException
        {
            return  DriverManager.getConnection(url,user,password);
        }
}
