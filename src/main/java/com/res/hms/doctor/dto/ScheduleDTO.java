package com.res.hms.doctor.dto;

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
public class ScheduleDTO {
  private UUID id;
  private Short dayOfWeek; // 0-6
  private LocalTime startTime;
  private LocalTime endTime;
  private Integer slotDurationMinutes;
  private Integer maxPatients;
}
