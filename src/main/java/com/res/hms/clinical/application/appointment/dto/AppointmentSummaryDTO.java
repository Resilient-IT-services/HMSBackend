package com.res.hms.clinical.application.appointment.dto;

import com.res.hms.clinical.domain.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSummaryDTO {
  private UUID id;
  private UUID patientId;
  private String patientName;
  private String patientCode;
  private UUID doctorId;
  private String doctorName;
  private String doctorSpecialization;
  private LocalDateTime appointmentTime;
  private AppointmentStatus status;
  private Integer tokenNumber;
}
