package com.res.hms.pharmacy.mapper;

import com.res.hms.domain.Medicine;
import com.res.hms.pharmacy.dto.MedicineDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MedicineMapper {
  @Mapping(target = "temperature", source = "storageTemperature")
  @Mapping(target = "humidity", source = "storageHumidity")
  MedicineDTO toDto(Medicine medicine);

  @Mapping(target = "storageTemperature", source = "temperature")
  @Mapping(target = "storageHumidity", source = "humidity")
  Medicine toEntity(MedicineDTO dto);

  @Mapping(target = "storageTemperature", source = "temperature")
  @Mapping(target = "storageHumidity", source = "humidity")
  void updateEntity(MedicineDTO dto, @MappingTarget Medicine medicine);
}
