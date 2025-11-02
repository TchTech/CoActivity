package BasicClasses;

import lombok.Data;

import java.time.Instant;
@Data
public class Notification {
  private Integer id;
  private User user;
  private String title;
  private String content;
  private boolean isRead;
  private Instant createdAt;
}
