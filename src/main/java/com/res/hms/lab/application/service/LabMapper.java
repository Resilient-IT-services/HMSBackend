package com.res.hms.lab.application.service;

import com.res.hms.lab.application.dto.LabDepartmentDTO;
import com.res.hms.lab.application.dto.LabTestDTO;
import com.res.hms.lab.domain.LabDepartment;
import com.res.hms.lab.domain.LabTest;
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
}
