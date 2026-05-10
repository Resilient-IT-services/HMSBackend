package com.res.hms.pharmacy.application.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PharmacySaleRequest {
  private UUID patientId;
  private UUID prescriptionId;

  @Valid @NotEmpty private List<Item> items;

  @Data
  public static class Item {
    @NotNull private UUID medicineId;

    @NotNull
    @Min(1)
    private Integer quantity;
  }
}
