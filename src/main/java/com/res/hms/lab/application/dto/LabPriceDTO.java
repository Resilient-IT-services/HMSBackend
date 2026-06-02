package com.res.hms.lab.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabPriceDTO {
    private UUID id;
    private UUID hospitalId;
    private UUID testId;
    private UUID packageId;
    private BigDecimal standardPrice;
    private BigDecimal emergencyPrice;
    private BigDecimal insurancePrice;
    private LocalDate effectiveFrom;
}
