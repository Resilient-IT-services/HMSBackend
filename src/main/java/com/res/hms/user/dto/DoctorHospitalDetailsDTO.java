package com.res.hms.user.dto;

import com.res.hms.domain.CommissionType;
import java.math.BigDecimal;
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
