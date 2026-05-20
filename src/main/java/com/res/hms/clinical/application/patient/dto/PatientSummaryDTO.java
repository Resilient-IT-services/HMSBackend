package com.res.hms.clinical.application.patient.dto;

import com.res.hms.core.domain.Gender;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientSummaryDTO {
  private UUID id;
  private String fullName;
  private String patientCode;
  private Gender gender;
  private java.time.LocalDate dob;
  private String phone;
}
