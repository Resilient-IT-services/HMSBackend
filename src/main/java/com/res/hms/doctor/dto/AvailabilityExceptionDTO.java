package com.res.hms.doctor.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
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
  private LocalDate date;
  private String type; // FULL_DAY_OFF, MODIFIED, ADDED
  private LocalTime startTime;
  private LocalTime endTime;
  private String mode;
  private Integer slotDurationMinutes;
  private String note;
}
