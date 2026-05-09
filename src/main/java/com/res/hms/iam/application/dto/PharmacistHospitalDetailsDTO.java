package com.res.hms.iam.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacistHospitalDetailsDTO {
  private String counterLocation;
  private String pharmacyWing;
  private String pharmacyLicenseNo;
}
