package com.res.hms.doctor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate date;

  private String type; // FULL_DAY_OFF, MODIFIED, ADDED

  @JsonFormat(pattern = "HH:mm")
  private LocalTime startTime;

  @JsonFormat(pattern = "HH:mm")
  private LocalTime endTime;

  private String mode;
  private Integer slotDurationMinutes;
  private String note;
}
