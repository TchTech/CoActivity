package com.mipt.CoActivity.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdateInterestsRequest {
  private List<String> interests;
}

