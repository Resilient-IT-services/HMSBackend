package com.res.hms.doctor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAvailabilityResponse {
  private List<SessionInfo> sessions;

  @Data
  @AllArgsConstructor
  public static class SessionInfo {
    private UUID scheduleId;

    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "09:00")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "11:00")
    private LocalTime endTime;

    private Integer maxPatients;
    private int currentBookings;
    private boolean available;

    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "09:30")
    private LocalTime nextSuggestedTime;
  }
}
