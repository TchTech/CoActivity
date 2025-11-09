package com.mipt.CoActivity.model;

import lombok.Data;

@Data
public class Interest {
  private String name;
  private InterestCategory category;
  private String description;
  private Integer id;
}
