package com.res.hms.appointment.dto;

import com.res.hms.domain.AppointmentStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
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
public class AppointmentDTO {
  private UUID id;

  @NotNull(message = "patientId is required")
  private UUID patientId;

  private String patientName;

  @NotNull(message = "doctorId is required")
  private UUID doctorId;

  private String doctorName;

  @NotNull(message = "appointmentTime is required")
  @Future(message = "appointmentTime must be in the future")
  private LocalDateTime appointmentTime;

  private AppointmentStatus status;
  private String notes;

  @jakarta.validation.Valid private VitalsDTO vitals;
}
