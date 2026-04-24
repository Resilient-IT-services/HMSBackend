package com.res.hms.user.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminHospitalDetailsDTO {
  private String officeLocation;
  private boolean departmentHead;
  private LocalDateTime updatedAt;
}
