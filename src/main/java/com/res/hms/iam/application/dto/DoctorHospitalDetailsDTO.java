package com.res.hms.iam.application.dto;

import java.math.BigDecimal;

import com.res.hms.finance.domain.CommissionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorHospitalDetailsDTO {
  private BigDecimal consultationFee;
  private Integer followUpDays;
  private CommissionType commissionType;
  private BigDecimal commissionValue;
  private boolean visitingConsultant;
}
