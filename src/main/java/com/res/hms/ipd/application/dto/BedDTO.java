package com.res.hms.ipd.application.dto;

import com.res.hms.ipd.domain.BedStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class BedDTO {
  private UUID id;

  @NotNull(message = "Ward ID is required")
  private UUID wardId;

  @NotBlank(message = "Bed number is required")
  private String bedNumber;

  private BedStatus status;
  private boolean active;
  private String wardName;
}
