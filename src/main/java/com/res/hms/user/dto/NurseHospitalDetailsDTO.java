package com.res.hms.user.dto;

import com.res.hms.domain.ShiftType;
import java.time.LocalDateTime;
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
  private LocalDateTime updatedAt;
}
