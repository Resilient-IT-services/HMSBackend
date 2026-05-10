package com.res.hms.iam.application.dto;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSlotDTO {
  @JsonFormat(pattern = "HH:mm")
  private LocalTime startTime;

  @JsonFormat(pattern = "HH:mm")
  private LocalTime endTime;

  private boolean available;
  private String status; // e.g., "AVAILABLE", "BOOKED", "SESSION_EXPIRED"
}
