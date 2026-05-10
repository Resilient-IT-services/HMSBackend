package com.res.hms.pharmacy.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockRequest {
  @NotNull private UUID medicineId;
  @NotBlank private String batchNumber;
  private LocalDate expiryDate;

  @NotNull
  @Min(1)
  private Integer quantity;

  @NotNull private BigDecimal purchasePrice;
  @NotNull private BigDecimal sellingPrice;
}
