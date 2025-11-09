package BasicClasses;

import lombok.Data;

import java.time.Instant;
import java.util.List;
@Data
public class Message {
  private Integer id;
  private Room room;
  private List<User> collaborators;
  private Instant date;
  private Instant dateCreated;
  private Boolean isDeleted;
  private User createdBy;
}
