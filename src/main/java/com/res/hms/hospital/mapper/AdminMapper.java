package com.res.hms.hospital.mapper;

import com.res.hms.domain.AdminHospitalDetails;
import com.res.hms.user.dto.AdminHospitalDetailsDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdminMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "assignment", ignore = true)
  void updateEntity(AdminHospitalDetailsDTO dto, @MappingTarget AdminHospitalDetails details);
}
