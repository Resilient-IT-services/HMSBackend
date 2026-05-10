package com.res.hms.clinical.application.appointment.dto;

import java.util.Map;

import com.res.hms.clinical.domain.AppointmentStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppointmentStatsDTO {
  private long total;
  private Map<AppointmentStatus, Long> byStatus;
}
