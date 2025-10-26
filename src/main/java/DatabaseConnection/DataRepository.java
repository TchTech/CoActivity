package DatabaseConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DataRepository {

  private final String url;
  private final String username;
  private final String password;

  public DataRepository(String url, String username, String password) {
    this.url = url;
    this.username = username;
    this.password = password;
  }

  public void execute(String sql) throws SQLException {

    try {
      Connection connection = DriverManager.getConnection(url, username, password);
      Statement statement = connection.createStatement();
      statement.execute(sql);

    } catch (SQLException e) {
      throw e;
    }
  }
}
