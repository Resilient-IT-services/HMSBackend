package com.res.hms.clinical.application.appointment.dto;

import com.res.hms.clinical.domain.AppointmentStatus;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppointmentStatsDTO {
  private long total;
  private long upcoming;
  private Map<AppointmentStatus, Long> byStatus;
}
