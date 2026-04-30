package com.res.hms.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineDTO {
  private UUID id;

  @NotBlank(message = "Medicine name is required")
  private String name;

  private String composition;
  private String manufacturer;
}
