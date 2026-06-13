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
import com.res.hms.lab.application.dto.LabOrderResponseDTO;
import com.res.hms.lab.application.dto.LabOrderTestResponseDTO;
import com.res.hms.lab.domain.LabOrder;
import com.res.hms.lab.domain.LabOrderTest;
import com.res.hms.lab.infrastructure.repository.LabPackageRepository;
import com.res.hms.lab.infrastructure.repository.LabTestRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LabMapper {

  default LabOrderResponseDTO toResponseDTO(
      LabOrder order,
      String patientName,
      List<LabOrderTest> orderTests,
      LabTestRepository testRepository,
      LabPackageRepository packageRepository) {

    List<LabOrderTestResponseDTO> testDTOs =
        orderTests.stream()
            .map(
                ot -> {
                  String testName = "";
                  String testCode = "";
                  if (ot.getTestId() != null) {
                    LabTest test = testRepository.findById(ot.getTestId()).orElse(null);
                    if (test != null) {
                      testName = test.getTestName();
                      testCode = test.getTestCode();
                    }
                  }

                  String packageName = "";
                  if (ot.getPackageId() != null) {
                    packageName =
                        packageRepository
                            .findById(ot.getPackageId())
                            .map(LabPackage::getPackageName)
                            .orElse("");
                  }

                  return LabOrderTestResponseDTO.builder()
                      .id(ot.getId())
                      .testId(ot.getTestId())
                      .testName(ot.getTestName() != null ? ot.getTestName() : testName)
                      .testCode(testCode)
                      .category(ot.getCategory() != null ? ot.getCategory().name() : null)
                      .resultEntryType(
                          ot.getResultEntryType() != null ? ot.getResultEntryType().name() : null)
                      .packageId(ot.getPackageId())
                      .packageName(packageName)
                      .status(ot.getStatus())
                      .build();
                })
            .collect(Collectors.toList());

    return LabOrderResponseDTO.builder()
        .id(order.getId())
        .patientId(order.getPatientId())
        .patientName(patientName)
        .orderNumber(order.getOrderNumber())
        .status(order.getStatus())
        .orderedAt(order.getOrderedAt())
        .tests(testDTOs)
        .build();
  }

  LabDepartmentDTO toDto(LabDepartment entity);

  LabDepartment toEntity(LabDepartmentDTO dto);

  @Mapping(target = "departmentName", source = "department.name")
  @Mapping(target = "resultType", source = "resultEntryType")
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
