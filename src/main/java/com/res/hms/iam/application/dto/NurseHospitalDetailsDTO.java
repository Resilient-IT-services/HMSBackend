package com.res.hms.iam.application.dto;

import com.res.hms.core.domain.ShiftType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NurseHospitalDetailsDTO {
  private String floorLocation;
  private ShiftType shiftType;
}
