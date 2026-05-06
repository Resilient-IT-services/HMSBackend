package com.res.hms.appointment.dto;

import com.res.hms.common.validation.OnCreate;
import com.res.hms.domain.AppointmentStatus;
import com.res.hms.domain.SessionPeriod;
import com.res.hms.vital.dto.VitalsDTO;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
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

  private com.res.hms.prescription.dto.PrescriptionDTO prescription;
}
