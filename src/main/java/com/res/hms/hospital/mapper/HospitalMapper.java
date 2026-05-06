package com.res.hms.hospital.mapper;

import com.res.hms.domain.Hospital;
import com.res.hms.hospital.dto.HospitalDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HospitalMapper {
  HospitalDTO toDto(Hospital hospital);
  Hospital toEntity(HospitalDTO dto);
  void updateEntity(HospitalDTO dto, @MappingTarget Hospital hospital);
}
