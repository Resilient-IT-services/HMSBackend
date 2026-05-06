package com.res.hms.pharmacy.mapper;

import com.res.hms.domain.PharmacistHospitalDetails;
import com.res.hms.domain.PharmacistProfile;
import com.res.hms.user.dto.PharmacistHospitalDetailsDTO;
import com.res.hms.user.dto.UserDetailsDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PharmacistMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  void updateEntity(UserDetailsDTO.PharmacistInfo info, @MappingTarget PharmacistProfile profile);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "assignment", ignore = true)
  void updateEntity(PharmacistHospitalDetailsDTO dto, @MappingTarget PharmacistHospitalDetails details);
}
