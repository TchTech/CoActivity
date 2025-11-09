package BasicClasses;
import lombok.Data;

import java.time.Instant;
import java.util.List;
@Data
public class User {
  private String name;
  private String eMail;
  private Integer id;
  private String passwordHash;
  private List<Room> rooms;
  private List<Interest> interests;
  private List<Feedback> feedbacks;
  private List<Feedback> feedbacksAuthor;
  private Instant createdAt;
}
