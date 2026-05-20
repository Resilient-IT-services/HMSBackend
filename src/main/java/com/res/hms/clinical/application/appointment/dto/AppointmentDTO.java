package com.res.hms.clinical.application.appointment.dto;

import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.clinical.domain.SessionPeriod;
import com.res.hms.core.validation.OnCreate;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
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
public class AppointmentDTO {
  private UUID id;

  @NotNull(message = "patientId is required")
  private UUID patientId;

  private String patientName;

  @NotNull(message = "doctorId is required")
  private UUID doctorId;

  private String doctorName;

  @NotNull(message = "appointmentTime is required")
  @FutureOrPresent(
      message = "appointmentTime must be current or future time",
      groups = OnCreate.class)
  private java.time.OffsetDateTime appointmentTime;

  private AppointmentStatus status;
  private Integer tokenNumber;
  private SessionPeriod sessionPeriod;
  private String chiefComplaints;
  private String diagnosis;
  private String clinicalNotes;
  private List<String> icdCodes;
  private String notes;

  @jakarta.validation.Valid private VitalsDTO vitals;

  private UUID visitId;

  private com.res.hms.clinical.application.prescription.dto.PrescriptionDTO prescription;
}
