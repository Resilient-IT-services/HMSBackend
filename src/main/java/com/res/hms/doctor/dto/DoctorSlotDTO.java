package com.res.hms.doctor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;
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
