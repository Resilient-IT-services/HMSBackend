package com.res.hms.clinical.application.patient.dto;

import java.util.List;

import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientHistoryDTO {
  private PatientDTO patient;
  private List<AppointmentDTO> visits;
}
