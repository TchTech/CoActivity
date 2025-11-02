package BasicClasses;

import lombok.Data;

import java.util.List;
@Data
public class Post {
  private User author;
  private Integer id;
  private String text;
  private List<User> likedUsers;
  private List<User> dislikedUsers;
  private List<Comment> comments;
  private Room room;
  private Image image;
}
