package com.res.hms.ipd.application.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Data
public class ProgressNoteResponse {
  private UUID id;
  private UUID doctorId;
  private String doctorName;
  private String note;
  private Instant createdAt;
}
