package com.res.hms.iam.application.dto;

import com.res.hms.core.domain.CertificationLevel;
import com.res.hms.core.domain.Gender;
import com.res.hms.core.domain.ShiftType;
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
  private UUID userId;
  private UUID nurseId;
  private UUID assignmentId;
  private String fullName;
  private String phone;
  private Gender gender;
  private String departmentName;
  private CertificationLevel certificationLevel;
  private ShiftType shiftType;
}
