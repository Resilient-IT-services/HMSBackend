package com.res.hms.pharmacy.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PharmacyReturnRequest {
  @NotNull private UUID saleItemId;

  @NotNull
  @Min(1)
  private Integer quantity;

  private String reason;
}
