package com.res.hms.user.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedAssignmentDTO {
  private UUID id;
  private UUID hospitalId;
  private String hospitalName;
  private String roleCode;
  private String departmentName;
  private boolean active;
  private LocalDateTime createdAt;
  // Hospital specific role details if applicable
  private BigDecimal consultationFee;
  private Integer followUpDays;

  private String officeLocation;
  private Boolean departmentHead;

  private String floorLocation;
  private String shiftType;

  private String counterLocation;
  private String pharmacyWing;

  private String deskLocation;
  private String emergencyContactNumber;
}
