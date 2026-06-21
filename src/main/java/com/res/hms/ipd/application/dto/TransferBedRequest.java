package com.res.hms.ipd.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class TransferBedRequest {
  @NotNull(message = "Target Bed ID is required")
  private UUID toBedId;

  private String transferReason;
  private String remarks;
}
