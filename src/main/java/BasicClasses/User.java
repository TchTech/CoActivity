package BasicClasses;
import lombok.Data;

import java.util.List;
import java.util.Date;
@Data
public class User {
  private String name;
  private String eMail;
  private Integer id;
  private String passwordHash;
  private List<Integer> rooms;
  private List<Integer> interests;
  private List<Integer> feedbacks;
  private List<Integer> feedbacksAuthor;
  private Date createdAt;
}
