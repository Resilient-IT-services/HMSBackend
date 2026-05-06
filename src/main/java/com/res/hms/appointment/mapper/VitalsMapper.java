package com.res.hms.appointment.mapper;

import com.res.hms.vital.dto.VitalsDTO;
import com.res.hms.domain.Vitals;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VitalsMapper {
  VitalsDTO toDto(Vitals vitals);
  Vitals toEntity(VitalsDTO dto);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "appointment", ignore = true)
  void updateEntity(VitalsDTO dto, @MappingTarget Vitals entity);
}
