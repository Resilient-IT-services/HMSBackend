package com.res.hms.clinical.application.appointment.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.clinical.domain.SessionPeriod;
import com.res.hms.core.validation.OnCreate;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
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
  @Future(message = "appointmentTime must be in the future", groups = OnCreate.class)
  private LocalDateTime appointmentTime;

  private AppointmentStatus status;
  private Integer tokenNumber;
  private SessionPeriod sessionPeriod;
  private String chiefComplaints;
  private String diagnosis;
  private String clinicalNotes;
  private List<String> icdCodes;
  private String notes;

  @jakarta.validation.Valid private VitalsDTO vitals;

  private com.res.hms.clinical.application.prescription.dto.PrescriptionDTO prescription;
}
