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
public class LabTestDTO {
  private UUID id;
  private UUID hospitalId;
  private String testCode;
  private String testName;
  private String shortName;
  private String aliasName;
  private UUID departmentId;
  private String departmentName;
  private String resultType;
  private Integer tatValue;
  private String tatUnit;
  private boolean active;
}
