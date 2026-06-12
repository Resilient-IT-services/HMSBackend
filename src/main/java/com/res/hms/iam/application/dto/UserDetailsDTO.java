package com.res.hms.iam.application.dto;

import com.res.hms.core.domain.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
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
  @Schema(type = "string", example = "1990-01-01")
  private LocalDate dob;

  private String phone;
  private Boolean active;
  private Boolean superuser;

  // Global Profile Info
  private DoctorInfo doctorInfo;
  private NurseInfo nurseInfo;
  private PharmacistInfo pharmacistInfo;
  private LabAdminInfo labAdminInfo;
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
    private List<String> departments;
    private Boolean active;
    private List<ScheduleDTO> schedules;
    private List<AvailabilityExceptionDTO> availabilityExceptions;

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
    private UUID id;
    private List<String> specializations;
    private DoctorEducation education;
    private String bio;
    private String licenseNumber;
    private String registrationNumber;
    private Integer experienceYears;
    private List<String> awards;
    private List<String> languages;
    private String signature;
    private java.math.BigDecimal consultationFee;
    private java.util.Map<String, Object> metadata;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NurseInfo {
    private UUID id;
    private String nursingLicenseNo;
    private com.res.hms.core.domain.CertificationLevel certificationLevel;
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
  public static class LabAdminInfo {
    private String specialty;
    private String licenseNo;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReceptionistInfo {
    private ReceptionistFluency languagesSpoken;
    private boolean insuranceSpecialist;
  }
}
