package com.res.hms.lab.application.service;

import com.res.hms.clinical.infrastructure.repository.PatientRepository;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.lab.application.dto.LabOrderCreateRequest;
import com.res.hms.lab.application.dto.LabOrderResponseDTO;
import com.res.hms.lab.application.dto.LabOrderTestResponseDTO;
import com.res.hms.lab.application.dto.LabResultSaveRequest;
import com.res.hms.lab.domain.LabOrder;
import com.res.hms.lab.domain.LabOrderTest;
import com.res.hms.lab.domain.LabPackage;
import com.res.hms.lab.domain.LabResult;
import com.res.hms.lab.domain.LabResultParameter;
import com.res.hms.lab.domain.LabSample;
import com.res.hms.lab.domain.LabTest;
import com.res.hms.lab.infrastructure.repository.LabOrderRepository;
import com.res.hms.lab.infrastructure.repository.LabOrderTestRepository;
import com.res.hms.lab.infrastructure.repository.LabPackageRepository;
import com.res.hms.lab.infrastructure.repository.LabResultParameterRepository;
import com.res.hms.lab.infrastructure.repository.LabResultRepository;
import com.res.hms.lab.infrastructure.repository.LabSampleRepository;
import com.res.hms.lab.infrastructure.repository.LabTestRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.hms.core.api.PageResponse;
import com.res.hms.lab.application.dto.LabQueueItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.jdbc.core.JdbcTemplate;
import com.res.hms.core.exception.ApiException;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.res.hms.lab.application.dto.LabTestParameterDTO;
import com.res.hms.lab.infrastructure.repository.LabTestParameterRepository;

@Service
@RequiredArgsConstructor
public class LabOrderService {

  private final LabOrderRepository orderRepository;
  private final LabOrderTestRepository orderTestRepository;
  private final LabTestRepository testRepository;
  private final LabPackageRepository packageRepository;
  private final PatientRepository patientRepository;
  private final LabSampleRepository sampleRepository;
  private final LabResultRepository resultRepository;
  private final LabResultParameterRepository resultParameterRepository;
  private final LabTestParameterRepository testParameterRepository;
  private final HospitalContext hospitalContext;
  private final JdbcTemplate jdbcTemplate;
  private final LabMapper mapper;

  @Transactional(readOnly = true)
  public List<LabTestParameterDTO> getParametersForOrderTest(UUID orderTestId) {
    LabOrderTest orderTest = orderTestRepository.findById(orderTestId)
        .orElseThrow(() -> new ApiException("NOT_FOUND", "Order test not found", HttpStatus.NOT_FOUND));
        
    if (orderTest.getTestId() == null) {
       // MVP: Packages not fully supported for parameter fetching here yet
       return List.of();
    }
    
    return testParameterRepository.findByTestIdOrderByDisplayOrderAsc(orderTest.getTestId())
        .stream()
        .map(mapper::toDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public PageResponse<LabQueueItemDTO> getLabQueue(String status, int page, int size) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Pageable pageable = PageRequest.of(page, size, Sort.by("orderedAt").descending());
    
    Page<LabQueueItemDTO> queue = orderTestRepository.findQueueByHospitalAndStatus(hospitalId, status, pageable);
    return new PageResponse<>(queue.getContent(), queue.getNumber(), queue.getSize(), queue.getTotalElements(), queue.getTotalPages());
  }

  @Transactional
  public String collectSample(UUID orderTestId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    UUID userId = hospitalContext.requireUserId();

    LabOrderTest orderTest = orderTestRepository.findById(orderTestId)
        .orElseThrow(() -> new ApiException("NOT_FOUND", "Order test not found", HttpStatus.NOT_FOUND));

    if (!"PENDING".equals(orderTest.getStatus())) {
        throw new ApiException("INVALID_STATE", "Sample already collected or processed", HttpStatus.BAD_REQUEST);
    }

    String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    Long seq = jdbcTemplate.queryForObject("SELECT nextval('lab.sample_seq')", Long.class);
    String sampleNumber = String.format("SMP-%s-%05d", datePart, seq);

    LabSample sample = new LabSample();
    sample.setHospitalId(hospitalId);
    sample.setOrderTestId(orderTestId);
    sample.setSampleNumber(sampleNumber);
    sample.setCollectedBy(userId);
    sample.setCollectedAt(Instant.now());
    sample.setStatus("COLLECTED");
    sampleRepository.save(sample);

    orderTest.setStatus("SAMPLE_COLLECTED");
    orderTestRepository.save(orderTest);

    return sampleNumber;
  }

  @Transactional
  public LabOrder createOrder(LabOrderCreateRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    LabOrder order =
        LabOrder.builder()
            .hospitalId(hospitalId)
            .patientId(request.getPatientId())
            .encounterId(request.getEncounterId())
            .orderNumber("ORD-" + System.currentTimeMillis())
            .status("ORDERED")
            .orderedAt(Instant.now())
            .build();

    order = orderRepository.save(order);

    if (request.getTestIds() != null) {
      for (UUID testId : request.getTestIds()) {
        orderTestRepository.save(
            LabOrderTest.builder().orderId(order.getId()).testId(testId).status("PENDING").build());
      }
    }

    if (request.getPackageIds() != null) {
      for (UUID packageId : request.getPackageIds()) {
        orderTestRepository.save(
            LabOrderTest.builder()
                .orderId(order.getId())
                .packageId(packageId)
                .status("PENDING")
                .build());
      }
    }

    return order;
  }

  public List<LabOrderResponseDTO> getQueue() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    List<LabOrder> orders =
        orderRepository.findAll().stream()
            .filter(o -> hospitalId.equals(o.getHospitalId()))
            .filter(o -> !"COMPLETED".equals(o.getStatus()))
            .collect(Collectors.toList());

    return orders.stream().map(this::mapToResponseDTO).collect(Collectors.toList());
  }

  @Transactional
  public void saveResults(LabResultSaveRequest request) {
    LabOrderTest orderTest =
        orderTestRepository
            .findById(request.getOrderTestId())
            .orElseThrow(() -> new RuntimeException("Order test not found"));

    UUID hospitalId = hospitalContext.requireHospitalId();

    // 1. Ensure sample exists
    LabSample sample =
        sampleRepository.findAll().stream()
            .filter(s -> orderTest.getId().equals(s.getOrderTestId()))
            .findFirst()
            .orElseGet(
                () ->
                    sampleRepository.save(
                        LabSample.builder()
                            .hospitalId(hospitalId)
                            .orderTestId(orderTest.getId())
                            .sampleNumber("SAM-" + System.currentTimeMillis())
                            .status("COLLECTED")
                            .collectedAt(Instant.now())
                            .build()));

    // 2. Create Result record
    LabResult result =
        resultRepository.save(
            LabResult.builder().sampleId(sample.getId()).enteredAt(Instant.now()).build());

    // 3. Save Parameter Results
    for (LabResultSaveRequest.ParameterResultDTO resDto : request.getResults()) {
      resultParameterRepository.save(
          LabResultParameter.builder()
              .resultId(result.getId())
              .parameterId(resDto.getParameterId())
              .resultValue(resDto.getValue())
              .abnormal(resDto.getAbnormal())
              .critical(resDto.getCritical())
              .resultStatus("COMPLETED")
              .build());
    }

    // 4. Update Order Test Status
    orderTest.setStatus("COMPLETED");
    orderTestRepository.save(orderTest);

    // 5. Update Order Status if all tests are done
    LabOrder order = orderRepository.findById(orderTest.getOrderId()).orElse(null);
    if (order != null) {
      boolean allDone =
          orderTestRepository.findAll().stream()
              .filter(ot -> order.getId().equals(ot.getOrderId()))
              .allMatch(ot -> "COMPLETED".equals(ot.getStatus()));

      if (allDone) {
        order.setStatus("COMPLETED");
        orderRepository.save(order);
      }
    }
  }

  private LabOrderResponseDTO mapToResponseDTO(LabOrder order) {
    String patientName =
        patientRepository
            .findById(order.getPatientId())
            .map(p -> p.getFullName())
            .orElse("Unknown Patient");

    List<LabOrderTest> orderTests =
        orderTestRepository.findAll().stream()
            .filter(ot -> order.getId().equals(ot.getOrderId()))
            .collect(Collectors.toList());

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
                      .testName(testName)
                      .testCode(testCode)
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
}
