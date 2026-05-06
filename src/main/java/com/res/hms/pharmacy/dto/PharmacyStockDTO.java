package com.res.hms.pharmacy.dto;

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
public class PharmacyStockDTO {
  private UUID id;
  private UUID medicineId;
  private String medicineName;
  private String genericName;
  private String composition;
  private String batchNumber;
  private LocalDate expiryDate;
  private Integer quantity;
  private Integer totalQuantity;
  private BigDecimal purchasePrice;
  private BigDecimal sellingPrice;
  private BigDecimal price;
}
