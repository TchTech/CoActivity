package BasicClasses;

import lombok.Data;

import java.util.Date;
@Data
public class Notification {
  private Integer id;
  private Integer userId;
  private String type;
  private String title;
  private String content;
  private boolean isRead;
  private Date createdAt;
}
