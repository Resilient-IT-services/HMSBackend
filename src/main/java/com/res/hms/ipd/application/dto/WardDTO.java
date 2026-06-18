package com.res.hms.ipd.application.dto;

import com.res.hms.ipd.domain.WardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class WardDTO {
  private UUID id;

  @NotBlank(message = "Ward code is required")
  private String wardCode;

  @NotBlank(message = "Ward name is required")
  private String wardName;

  @NotNull(message = "Ward type is required")
  private WardType wardType;

  private Integer totalBeds;
  private boolean active;
  private String remarks;
}
