package com.res.hms.user.dto;

import com.res.hms.doctor.dto.DoctorEducation;
import com.res.hms.domain.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsDTO {
  private UUID id;

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
  private Boolean active;
  private Boolean superuser;
  private LocalDateTime createdAt;

  // Global Profile Info
  private DoctorInfo doctorInfo;
  private NurseInfo nurseInfo;
  private PharmacistInfo pharmacistInfo;
  private LabTechInfo labTechInfo;
  private ReceptionistInfo receptionistInfo;

  // Hospital Specific Memberships (Consolidated structure)
  private List<HospitalMembershipDTO> memberships;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HospitalMembershipDTO {
    private UUID hospitalId;
    private String hospitalName;
    private String hospitalCode;
    private List<RoleAssignmentDTO> roleAssignments;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RoleAssignmentDTO {
    private UUID assignmentId;
    private UUID hospitalId;
    private String roleCode;
    private String departmentName;
    private Boolean active;

    // Explicit role-specific hospital details
    private DoctorHospitalDetailsDTO doctorDetails;
    private NurseHospitalDetailsDTO nurseDetails;
    private AdminHospitalDetailsDTO adminDetails;
    private PharmacistHospitalDetailsDTO pharmacistDetails;
    private ReceptionistHospitalDetailsDTO receptionistDetails;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DoctorInfo {
    private String specialization;
    private DoctorEducation education;
    private String bio;
    private String licenseNumber;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NurseInfo {
    private String nursingLicenseNo;
    private com.res.hms.domain.CertificationLevel certificationLevel;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PharmacistInfo {
    private String pciRegNo;
    private String qualification;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LabTechInfo {
    private String specialty;
    private String licenseNo;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReceptionistInfo {
    private com.res.hms.user.dto.ReceptionistFluency languagesSpoken;
    private boolean insuranceSpecialist;
  }
}
