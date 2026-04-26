package com.res.hms.doctor.dto;

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
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxPatients;
    private int currentBookings;
    private boolean available;
    private LocalTime nextSuggestedTime;
  }
}
