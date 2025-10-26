package BasicClasses;

import lombok.Data;

import java.util.Date;
import java.util.List;
@Data
public class Message {
  private Integer id;
  private Integer roomId;
  private List<Integer> collaborators;
  private Date date;
  private Date dateCreated;
  private Boolean deleted;
  private Integer createdBy;
}
