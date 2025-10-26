package BasicClasses;

import lombok.Data;

import java.util.List;
@Data
public class Comment {
  private Integer authorId;
  private Integer id;
  private String text;
  private List<Integer> likedUsers;
  private List<Integer> dislikedUsers;
  private Integer post;
}
