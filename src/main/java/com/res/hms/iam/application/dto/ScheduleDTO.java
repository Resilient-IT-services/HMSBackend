package com.res.hms.iam.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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

  @JsonFormat(pattern = "HH:mm")
  @Schema(type = "string", pattern = "HH:mm", example = "09:00")
  private LocalTime startTime;

  @JsonFormat(pattern = "HH:mm")
  @Schema(type = "string", pattern = "HH:mm", example = "17:00")
  private LocalTime endTime;

  private Integer slotDurationMinutes;
  private Integer maxPatients;
}
