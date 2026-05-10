package com.res.hms.iam.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityExceptionDTO {
  private UUID id;

  @JsonFormat(pattern = "yyyy-MM-dd")
  @Schema(type = "string", example = "2026-05-01")
  private LocalDate date;

  private String type; // FULL_DAY_OFF, MODIFIED, ADDED

  @JsonFormat(pattern = "HH:mm")
  @Schema(type = "string", example = "14:00")
  private LocalTime startTime;

  @JsonFormat(pattern = "HH:mm")
  @Schema(type = "string", example = "16:00")
  private LocalTime endTime;

  private String mode;
  private Integer slotDurationMinutes;
  private String note;
}
