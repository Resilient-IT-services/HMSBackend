package com.res.hms.iam.application.dto;

import com.res.hms.core.domain.Gender;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
  private List<String> departments;
  private List<String> specializations;
  private BigDecimal consultationFee;
}
