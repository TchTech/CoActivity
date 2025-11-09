package BasicClasses;

import lombok.Data;

import java.util.List;
@Data
public class Comment {
  private User author;
  private Integer id;
  private String text;
  private List<User> likedUsers;
  private List<User> dislikedUsers;
  private Post commentOn;
}
