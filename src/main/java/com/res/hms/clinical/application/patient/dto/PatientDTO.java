package com.res.hms.clinical.application.patient.dto;

import com.res.hms.clinical.domain.BloodGroup;
import com.res.hms.core.domain.Gender;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDTO {
  private UUID id;

  @NotBlank(message = "fullName is required")
  private String fullName;

  private String patientCode;
  private String email;
  private String medicalHistory;
  private Gender gender;
  private LocalDate dob;
  private String phone;
  private BloodGroup bloodGroup;
  private String address;
}
