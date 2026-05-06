package com.res.hms.doctor.mapper;

import com.res.hms.doctor.dto.AvailabilityExceptionDTO;
import com.res.hms.doctor.dto.ScheduleDTO;
import com.res.hms.domain.DoctorAvailabilityException;
import com.res.hms.domain.DoctorHospitalDetails;
import com.res.hms.domain.DoctorProfile;
import com.res.hms.domain.DoctorSchedule;
import com.res.hms.user.dto.DoctorHospitalDetailsDTO;
import com.res.hms.user.dto.UserDetailsDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DoctorMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "availabilityExceptions", ignore = true)
  void updateEntity(UserDetailsDTO.DoctorInfo info, @MappingTarget DoctorProfile profile);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "assignment", ignore = true)
  void updateEntity(DoctorHospitalDetailsDTO dto, @MappingTarget DoctorHospitalDetails details);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "assignment", ignore = true)
  void updateEntity(ScheduleDTO dto, @MappingTarget DoctorSchedule schedule);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "doctorProfile", ignore = true)
  @Mapping(target = "hospital", ignore = true)
  void updateEntity(AvailabilityExceptionDTO dto, @MappingTarget DoctorAvailabilityException exception);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "assignment", ignore = true)
  DoctorSchedule toScheduleEntity(ScheduleDTO dto);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "doctorProfile", ignore = true)
  @Mapping(target = "hospital", ignore = true)
  DoctorAvailabilityException toExceptionEntity(AvailabilityExceptionDTO dto);
}
