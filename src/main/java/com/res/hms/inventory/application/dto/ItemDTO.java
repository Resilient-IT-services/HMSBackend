package com.res.hms.inventory.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDTO {
  private UUID id;

  @NotBlank(message = "Item name is required")
  private String name;

  private String category;
  private String unit;
}
