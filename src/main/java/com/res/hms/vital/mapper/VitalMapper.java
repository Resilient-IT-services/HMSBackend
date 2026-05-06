package com.res.hms.vital.mapper;

import com.res.hms.domain.VitalRangeConfig;
import com.res.hms.vital.dto.VitalRangeConfigDTO;
import com.res.hms.vital.dto.VitalSettingDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VitalMapper {
  VitalRangeConfigDTO toDto(VitalRangeConfig config);
  void updateEntity(VitalRangeConfigDTO dto, @MappingTarget VitalRangeConfig config);

  @Mapping(target = "vitalName", source = "vitalType")
  VitalSettingDTO toSettingDto(VitalRangeConfig config);

  @Mapping(target = "vitalType", source = "vitalName")
  void updateEntity(VitalSettingDTO dto, @MappingTarget VitalRangeConfig config);
}
