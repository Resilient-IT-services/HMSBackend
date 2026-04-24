package com.res.hms.user.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private String commissionType;
    private BigDecimal commissionValue;
    private boolean visitingConsultant;
    private LocalDateTime updatedAt;
}
