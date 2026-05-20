package com.res.hms.clinical.application.appointment.dto;

import com.res.hms.finance.domain.PaymentMode;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

@Data
public class OPDEncounterRequest {
  @NotNull private UUID patientId;
  @NotNull private UUID doctorId;
  @NotNull private OffsetDateTime appointmentTime;
  private String notes;

  // Optional Vitals
  private VitalsDTO vitals;

  // Immediate Payment Info
  private PaymentInfo payment;

  @Data
  public static class PaymentInfo {
    @NotNull private BigDecimal amount;
    @NotNull private PaymentMode mode;
    private String referenceNumber;
  }

  public AppointmentDTO toAppointmentDTO(UUID visitId) {
    AppointmentDTO dto = new AppointmentDTO();
    dto.setPatientId(patientId);
    dto.setDoctorId(doctorId);
    dto.setAppointmentTime(appointmentTime);
    dto.setNotes(notes);
    dto.setVisitId(visitId);
    dto.setVitals(vitals);
    return dto;
  }
}
