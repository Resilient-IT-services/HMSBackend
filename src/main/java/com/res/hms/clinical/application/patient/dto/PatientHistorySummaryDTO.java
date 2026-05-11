package com.res.hms.clinical.application.patient.dto;

import com.res.hms.clinical.application.appointment.dto.AppointmentSummaryDTO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientHistorySummaryDTO {
  private PatientSummaryDTO patient;
  private List<AppointmentSummaryDTO> visits;
}
