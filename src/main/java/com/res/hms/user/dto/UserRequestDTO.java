package com.res.hms.user.dto;

import com.res.hms.domain.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO {
  @NotBlank(message = "username is required")
  private String username;

  @Email(message = "invalid email format")
  private String email;

  private String password;

  @NotBlank(message = "fullName is required")
  private String fullName;

  @NotNull(message = "gender is required")
  private Gender gender;

  @NotNull(message = "dob is required")
  private LocalDate dob;

  private String phone;
  private Boolean superuser;

  // Global Profile Info (Mirrors UserFullProfileDTO)
  private UserFullProfileDTO.DoctorInfo doctorInfo;
  private UserFullProfileDTO.NurseInfo nurseInfo;
  private UserFullProfileDTO.PharmacistInfo pharmacistInfo;
  private UserFullProfileDTO.LabTechInfo labTechInfo;
  private UserFullProfileDTO.ReceptionistInfo receptionistInfo;

  // Hospital Specific Info (Mirrors UserFullProfileDTO)
  private List<UserFullProfileDTO.DetailedHospitalMembershipDTO> detailedMemberships;
}
