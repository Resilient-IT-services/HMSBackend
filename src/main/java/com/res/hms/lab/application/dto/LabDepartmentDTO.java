package com.res.hms.lab.application.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabDepartmentDTO {
  private UUID id;
  private UUID hospitalId;
  private String code;
  private String name;
  private boolean active;
}
