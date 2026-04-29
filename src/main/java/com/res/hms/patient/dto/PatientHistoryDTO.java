package com.res.hms.patient.dto;

import com.res.hms.appointment.dto.AppointmentDTO;
import java.util.List;
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
