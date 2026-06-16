package com.res.hms.pharmacy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyStockStatsDTO {
  private long totalBatches;
  private long totalMedicines;
  private long lowStockCount;
  private long expiringSoonCount;
}
