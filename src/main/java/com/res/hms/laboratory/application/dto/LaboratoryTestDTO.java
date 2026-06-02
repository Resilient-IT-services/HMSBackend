package com.res.hms.laboratory.application.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaboratoryTestDTO {
  private UUID id;
  private String testCode;
  private String testName;
  private String shortName;
  private String aliasName;
  private boolean active;
  private Instant createdAt;
  private Instant updatedAt;
}
