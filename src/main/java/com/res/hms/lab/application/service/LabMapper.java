package com.res.hms.lab.application.service;

import com.res.hms.lab.application.dto.LabDepartmentDTO;
import com.res.hms.lab.application.dto.LabInventoryItemDTO;
import com.res.hms.lab.application.dto.LabMachineDTO;
import com.res.hms.lab.application.dto.LabPackageDTO;
import com.res.hms.lab.application.dto.LabPriceDTO;
import com.res.hms.lab.application.dto.LabReferenceRangeDTO;
import com.res.hms.lab.application.dto.LabTestDTO;
import com.res.hms.lab.application.dto.LabTestParameterDTO;
import com.res.hms.lab.domain.LabDepartment;
import com.res.hms.lab.domain.LabInventoryItem;
import com.res.hms.lab.domain.LabMachine;
import com.res.hms.lab.domain.LabPackage;
import com.res.hms.lab.domain.LabPrice;
import com.res.hms.lab.domain.LabReferenceRange;
import com.res.hms.lab.domain.LabTest;
import com.res.hms.lab.domain.LabTestParameter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LabMapper {

  LabDepartmentDTO toDto(LabDepartment entity);

  LabDepartment toEntity(LabDepartmentDTO dto);

  @Mapping(target = "departmentName", source = "department.name")
  LabTestDTO toDto(LabTest entity);

  @Mapping(target = "departmentId", source = "departmentId")
  LabTest toEntity(LabTestDTO dto);

  LabTestParameterDTO toDto(LabTestParameter entity);

  LabTestParameter toEntity(LabTestParameterDTO dto);

  LabReferenceRangeDTO toDto(LabReferenceRange entity);

  LabReferenceRange toEntity(LabReferenceRangeDTO dto);

  LabPriceDTO toDto(LabPrice entity);

  LabPrice toEntity(LabPriceDTO dto);

  LabPackageDTO toDto(LabPackage entity);

  LabPackage toEntity(LabPackageDTO dto);

  LabInventoryItemDTO toDto(LabInventoryItem entity);

  LabInventoryItem toEntity(LabInventoryItemDTO dto);

  LabMachineDTO toDto(LabMachine entity);

  LabMachine toEntity(LabMachineDTO dto);
}
