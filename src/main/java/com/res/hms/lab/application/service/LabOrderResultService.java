package com.res.hms.lab.application.service;

import com.res.hms.clinical.domain.PrescriptionLabItemStatus;
import com.res.hms.core.exception.ApiException;
import com.res.hms.lab.application.dto.LabPackageResultBundleDTO;
import com.res.hms.lab.application.dto.LabPackageResultSaveRequest;
import com.res.hms.lab.application.dto.LabResultSaveRequest;
import com.res.hms.lab.application.dto.LabTestParameterDTO;
import com.res.hms.lab.domain.LabOrderTest;
import com.res.hms.lab.domain.LabPackage;
import com.res.hms.lab.domain.LabResult;
import com.res.hms.lab.infrastructure.repository.LabOrderTestRepository;
import com.res.hms.lab.infrastructure.repository.LabPackageRepository;
import com.res.hms.lab.infrastructure.repository.LabResultParameterRepository;
import com.res.hms.lab.infrastructure.repository.LabTestParameterRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabOrderResultService {

  private final LabOrderTestRepository orderTestRepository;
  private final LabPackageRepository packageRepository;
  private final LabResultService labResultService;
  private final LabOrderStatusService statusService;
  private final LabTestParameterRepository testParameterRepository;
  private final LabResultParameterRepository resultParameterRepository;
  private final LabMapper mapper;

  @Transactional(readOnly = true)
  public List<LabTestParameterDTO> getParametersForOrderTest(UUID orderTestId) {
    LabOrderTest orderTest =
        orderTestRepository
            .findById(orderTestId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Order test not found", HttpStatus.NOT_FOUND));

    if (orderTest.getTestId() == null) {
      return List.of();
    }

    LabResult existingResult = labResultService.findResultByOrderTestId(orderTestId);

    return testParameterRepository
        .findByTestIdOrderByDisplayOrderAsc(orderTest.getTestId())
        .stream()
        .map(
            param -> {
              LabTestParameterDTO dto = mapper.toDto(param);
              attachExistingResultToDto(dto, existingResult, param.getId());
              return dto;
            })
        .collect(Collectors.toList());
  }

  private void attachExistingResultToDto(
      LabTestParameterDTO dto, LabResult existingResult, UUID parameterId) {
    if (existingResult != null) {
      resultParameterRepository.findAll().stream()
          .filter(
              rp ->
                  existingResult.getId().equals(rp.getResultId())
                      && parameterId.equals(rp.getParameterId()))
          .findFirst()
          .ifPresent(
              rp -> {
                dto.setResultValue(rp.getResultValue());
                dto.setAbnormal(rp.getAbnormal());
                dto.setCritical(rp.getCritical());
              });
    }
  }

  @Transactional
  public void saveResults(LabResultSaveRequest request) {
    LabOrderTest orderTest =
        orderTestRepository
            .findById(request.getOrderTestId())
            .orElseThrow(() -> new RuntimeException("Order test not found"));

    LabResult result = labResultService.saveOrUpdateLabResult(orderTest, request.getFindings());
    labResultService.processResultParameters(result.getId(), request.getResults());

    orderTest.setStatus("RESULT_ENTERED");
    orderTestRepository.save(orderTest);
    statusService.syncPrescriptionStatus(
        request.getOrderTestId(), PrescriptionLabItemStatus.IN_PROGRESS);
  }

  @Transactional(readOnly = true)
  public LabPackageResultBundleDTO getPackageResultBundle(UUID orderId, UUID packageId) {
    LabPackage labPackage =
        packageRepository
            .findById(packageId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Package not found", HttpStatus.NOT_FOUND));

    List<LabOrderTest> components =
        orderTestRepository.findAll().stream()
            .filter(ot -> orderId.equals(ot.getOrderId()) && packageId.equals(ot.getPackageId()))
            .collect(Collectors.toList());

    String status = "PENDING";
    if (!components.isEmpty()) {
      boolean allVerified = components.stream().allMatch(c -> "VERIFIED".equals(c.getStatus()));
      boolean allResultEntered =
          components.stream()
              .allMatch(
                  c -> "VERIFIED".equals(c.getStatus()) || "RESULT_ENTERED".equals(c.getStatus()));
      boolean anyProcessing = components.stream().anyMatch(c -> "PROCESSING".equals(c.getStatus()));
      boolean anyCollected =
          components.stream().anyMatch(c -> "SAMPLE_COLLECTED".equals(c.getStatus()));

      if (allVerified) {
        status = "VERIFIED";
      } else if (allResultEntered) {
        status = "COMPLETED";
      } else if (anyProcessing) {
        status = "PROCESSING";
      } else if (anyCollected) {
        status = "COLLECTED";
      }
    }

    List<LabPackageResultBundleDTO.PackageTestBundleDTO> testBundles =
        components.stream()
            .map(
                comp -> {
                  LabResult existingResult = labResultService.findResultByOrderTestId(comp.getId());
                  String findings = existingResult != null ? existingResult.getFindings() : null;

                  List<LabTestParameterDTO> params = getParametersForOrderTest(comp.getId());

                  return LabPackageResultBundleDTO.PackageTestBundleDTO.builder()
                      .orderTestId(comp.getId())
                      .testId(comp.getTestId())
                      .testName(comp.getTestName())
                      .category(comp.getCategory() != null ? comp.getCategory().name() : null)
                      .resultEntryType(
                          comp.getResultEntryType() != null
                              ? comp.getResultEntryType().name()
                              : null)
                      .status(comp.getStatus())
                      .findings(findings)
                      .parameters(params)
                      .build();
                })
            .collect(Collectors.toList());

    return LabPackageResultBundleDTO.builder()
        .orderId(orderId)
        .packageId(packageId)
        .packageName(labPackage.getPackageName())
        .status(status)
        .tests(testBundles)
        .build();
  }

  @Transactional
  public void savePackageResults(LabPackageResultSaveRequest request) {
    if (request.getTests() != null) {
      for (LabPackageResultSaveRequest.PackageTestResultRequest testRequest : request.getTests()) {
        LabResultSaveRequest singleRequest =
            LabResultSaveRequest.builder()
                .orderTestId(testRequest.getOrderTestId())
                .findings(testRequest.getFindings())
                .results(testRequest.getResults())
                .build();
        saveResults(singleRequest);
      }
    }
  }
}
