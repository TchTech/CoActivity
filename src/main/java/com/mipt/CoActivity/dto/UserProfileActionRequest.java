package com.mipt.CoActivity.dto;

import lombok.Data;

@Data
public class UserProfileActionRequest {
  private String action;
  private Long targetUserId;
}

