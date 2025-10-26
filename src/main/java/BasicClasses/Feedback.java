package BasicClasses;

import lombok.Data;

@Data
public class Feedback {
  private Integer authorId;
  private Integer id;
  private String text;
  private Integer userId;
}
