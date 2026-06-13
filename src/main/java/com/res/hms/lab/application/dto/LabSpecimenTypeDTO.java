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
public class LabSpecimenTypeDTO {
  private UUID id;
  private String code;
  private String name;
}
