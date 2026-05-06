package com.res.hms.nurse.mapper;

import com.res.hms.domain.NurseHospitalDetails;
import com.res.hms.domain.NurseProfile;
import com.res.hms.user.dto.NurseHospitalDetailsDTO;
import com.res.hms.user.dto.UserDetailsDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NurseMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  void updateEntity(UserDetailsDTO.NurseInfo info, @MappingTarget NurseProfile profile);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "assignment", ignore = true)
  void updateEntity(NurseHospitalDetailsDTO dto, @MappingTarget NurseHospitalDetails details);
}
