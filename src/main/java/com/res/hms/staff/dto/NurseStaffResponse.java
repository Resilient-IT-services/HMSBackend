package com.res.hms.staff.dto;

import com.res.hms.domain.CertificationLevel;
import com.res.hms.domain.Gender;
import com.res.hms.domain.ShiftType;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NurseStaffResponse {
  private UUID id; // Assignment ID
  private UUID userId;
  private String fullName;
  private String phone;
  private Gender gender;
  private String departmentName;
  private CertificationLevel certificationLevel;
  private ShiftType shiftType;
}
