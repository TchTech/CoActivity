package com.mipt.CoActivity.model;

import lombok.Data;

@Data
public class Feedback {
  private User author;
  private Integer id;
  private String text;
  private User reviewedUser;
}
