package com.res.hms.patient.mapper;

import com.res.hms.domain.Patient;
import com.res.hms.patient.dto.PatientDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PatientMapper {
  PatientDTO toDto(Patient patient);
  Patient toEntity(PatientDTO dto);
  void updateEntity(PatientDTO dto, @MappingTarget Patient patient);
}
