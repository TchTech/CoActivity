package BasicClasses;

import lombok.Data;

import java.util.List;
@Data
public class Post {
  private Integer authorId;
  private Integer id;
  private String text;
  private List<Integer> likedUsers;
  private List<Integer> dislikedUsers;
  private List<Integer> comments;
  private Integer room;
  private String image;
}
