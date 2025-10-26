package BasicClasses;

import lombok.Data;

import java.util.List;
import java.util.Date;
@Data
public class Room {
  private List<Integer> AdminID;
  private Integer id;
  private String description;
  private List<Integer> collaborators;
  private Integer interestType;
  private String geoposition;
  private Integer createdBy;
  private Date createdAt;
  private Integer maxCollaborators;
}
