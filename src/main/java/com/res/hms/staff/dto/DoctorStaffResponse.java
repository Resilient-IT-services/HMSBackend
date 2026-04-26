package com.res.hms.staff.dto;

import com.res.hms.domain.Gender;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorStaffResponse {
  private UUID userId;
  private UUID doctorId;
  private UUID assignmentId;
  private String fullName;
  private String phone;
  private Gender gender;
  private String departmentName;
  private String specialization;
  private BigDecimal consultationFee;
}
