package docker;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public final class DataRepository {
    private final String url;
    private final String user;
    private final String password;

    public DataRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public int execute(String sql) throws SQLException {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement st = conn.createStatement()) {
            return st.executeUpdate(sql);
        }
    }

    public static void main(String[] args) throws Exception {
        String url, user, pass;
        try (BufferedReader reader = new BufferedReader(new FileReader("data.txt"))) {
            url  = reader.readLine();
            user = reader.readLine();
            pass = reader.readLine();
        }
        DataRepository repo = new DataRepository(url, user, pass);
        int res = repo.execute("CREATE TABLE IF NOT EXISTS _healthcheck(id INT)");
        System.out.println("OK, result=" + res);
    }
}
