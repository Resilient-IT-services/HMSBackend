package com.res.hms.ipd.application.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Data
public class BedTransferResponse {
  private UUID id;
  private UUID admissionId;
  private UUID fromBedId;
  private String fromBedNumber;
  private String fromWardName;
  private UUID toBedId;
  private String toBedNumber;
  private String toWardName;
  private String transferReason;
  private String remarks;
  private UUID transferredBy;
  private String transferredByName;
  private Instant transferredAt;
}
