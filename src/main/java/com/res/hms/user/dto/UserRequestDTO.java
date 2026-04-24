package com.res.hms.user.dto;

import com.res.hms.domain.CertificationLevel;
import com.res.hms.domain.CommissionType;
import com.res.hms.domain.Gender;
import com.res.hms.domain.RoleCode;
import com.res.hms.domain.ShiftType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
  private List<MembershipDTO> memberships;
  private Boolean superuser;

  // Doctor specific
  private String specialization;
  private BigDecimal consultationFee;
  private Integer followUpDays;
  private String bio;
  private String licenseNumber;

  // Nurse specific
  private String department;
  private String nursingLicenseNo;
  private CertificationLevel certificationLevel;

  // Pharmacist specific
  private String pciRegNo;
  private String qualification;

  // Lab Tech specific
  private String labSpecialty;
  private String labLicenseNo;

  // Receptionist specific
  private com.res.hms.user.dto.ReceptionistFluency languagesSpoken;
  private Boolean insuranceSpecialist;

  // Role-Hospital specific details (active hospital context)
  private String officeLocation;
  private Boolean departmentHead;
  private String floorLocation;
  private ShiftType shiftType;
  private String counterLocation;
  private String pharmacyWing;
  private String deskLocation;
  private String emergencyContactNumber;
  private CommissionType commissionType;
  private BigDecimal commissionValue;
  private Boolean visitingConsultant;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MembershipDTO {
    private UUID hospitalId;
    private Set<RoleCode> roles;
  }
}
