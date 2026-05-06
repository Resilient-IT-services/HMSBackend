package com.res.hms.patient.mapper;

import com.res.hms.appointment.dto.AppointmentDTO;
import com.res.hms.patient.dto.PatientDTO;
import com.res.hms.patient.dto.PatientHistoryDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PatientHistoryMapper {
  PatientHistoryDTO toDto(PatientDTO patient, List<AppointmentDTO> visits);
}
