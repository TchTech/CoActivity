package BasicClasses;

import lombok.Data;

import java.time.Instant;
import java.util.List;
@Data
public class Room {
  private List<User> admins;
  private Integer id;
  private String description;
  private List<User> collaborators;
  private InterestCategory interestType;
  private String geoposition;
  private User createdBy;
  private Instant createdAt;
  private Integer maxCollaborators;
}
