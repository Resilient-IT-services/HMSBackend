package com.res.hms.staff.mapper;

import com.res.hms.domain.LabTechProfile;
import com.res.hms.domain.ReceptionistHospitalDetails;
import com.res.hms.domain.ReceptionistProfile;
import com.res.hms.user.dto.ReceptionistHospitalDetailsDTO;
import com.res.hms.user.dto.UserDetailsDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StaffInternalMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  void updateEntity(UserDetailsDTO.LabTechInfo info, @MappingTarget LabTechProfile profile);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  void updateEntity(UserDetailsDTO.ReceptionistInfo info, @MappingTarget ReceptionistProfile profile);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "assignment", ignore = true)
  void updateEntity(ReceptionistHospitalDetailsDTO dto, @MappingTarget ReceptionistHospitalDetails details);
}
