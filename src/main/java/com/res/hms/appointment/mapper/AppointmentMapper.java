package com.res.hms.appointment.mapper;

import com.res.hms.appointment.dto.AppointmentDTO;
import com.res.hms.domain.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentMapper {
  @Mapping(target = "patientId", source = "patient.id")
  @Mapping(target = "patientName", source = "patient.fullName")
  @Mapping(target = "doctorId", source = "doctorProfile.id")
  @Mapping(target = "doctorName", source = "doctorProfile.fullName")
  AppointmentDTO toDto(Appointment appointment);

  @Mapping(target = "patient", ignore = true)
  @Mapping(target = "doctorProfile", ignore = true)
  Appointment toEntity(AppointmentDTO dto);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "patient", ignore = true)
  @Mapping(target = "doctorProfile", ignore = true)
  @Mapping(target = "vitals", ignore = true)
  @Mapping(target = "prescription", ignore = true)
  void updateEntity(AppointmentDTO dto, @MappingTarget Appointment appointment);
}
