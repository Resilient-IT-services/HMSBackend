package com.res.hms.user.dto;

import com.res.hms.doctor.dto.DoctorEducation;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserFullProfileDTO extends UserSummaryDTO {

  private DoctorInfo doctorInfo;
  private NurseInfo nurseInfo;
  private PharmacistInfo pharmacistInfo;
  private LabTechInfo labTechInfo;
  private ReceptionistInfo receptionistInfo;

  private List<DetailedHospitalMembershipDTO> detailedMemberships;

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DetailedHospitalMembershipDTO {
    private UUID hospitalId;
    private String hospitalName;
    private List<DetailedRoleAssignmentDTO> roleAssignments;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DetailedRoleAssignmentDTO {
    private UUID assignmentId;
    private String roleCode;
    private String departmentName;
    private boolean active;

    // Explicit role-specific hospital details
    private DoctorHospitalDetailsDTO doctorDetails;
    private NurseHospitalDetailsDTO nurseDetails;
    private AdminHospitalDetailsDTO adminDetails;
    private PharmacistHospitalDetailsDTO pharmacistDetails;
    private ReceptionistHospitalDetailsDTO receptionistDetails;
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DoctorInfo {
    private String specialization;
    private DoctorEducation education;
    private String bio;
    private String licenseNumber;
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NurseInfo {
    private String department;
    private String nursingLicenseNo;
    private com.res.hms.domain.CertificationLevel certificationLevel;
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PharmacistInfo {
    private String pciRegNo;
    private String qualification;
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LabTechInfo {
    private String specialty;
    private String licenseNo;
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReceptionistInfo {
    private com.res.hms.user.dto.ReceptionistFluency languagesSpoken;
    private boolean insuranceSpecialist;
  }
}
