package docker;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class DataRepository implements CommandLineRunner {

  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public DataRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(String... args) {
    try {
      jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS test_table (id SERIAL PRIMARY KEY, name TEXT)");
      jdbcTemplate.update("INSERT INTO test_table (name) VALUES (?)", "Hello World!");
      List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM test_table");
      for (Map<String, Object> row : rows) {
        System.out.println("id=" + row.get("id") + ", name=" + row.get("name"));
      }

    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
    }
  }

  public static void main(String[] args) {
    SpringApplication.run(DataRepository.class, args);
  }
}
