package com.res.hms.doctor.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnavailabilityDTO {
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String reason;
}
