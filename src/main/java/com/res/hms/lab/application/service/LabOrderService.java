package com.res.hms.lab.application.service;

import com.res.hms.clinical.domain.PrescriptionLabItemStatus;
import com.res.hms.clinical.infrastructure.repository.PatientRepository;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.lab.application.dto.LabOrderCreateRequest;
import com.res.hms.lab.application.dto.LabOrderResponseDTO;
import com.res.hms.lab.application.dto.LabPackageResultBundleDTO;
import com.res.hms.lab.application.dto.LabPackageResultSaveRequest;
import com.res.hms.lab.application.dto.LabQueueItemDTO;
import com.res.hms.lab.application.dto.LabResultSaveRequest;
import com.res.hms.lab.application.dto.LabTestParameterDTO;
import com.res.hms.lab.application.dto.WalkInLabOrderRequest;
import com.res.hms.lab.domain.LabOrder;
import com.res.hms.lab.domain.LabOrderTest;
import com.res.hms.lab.domain.LabPackageTest;
import com.res.hms.lab.domain.LabSample;
import com.res.hms.lab.domain.LabSpecimenType;
import com.res.hms.lab.domain.LabTestSpecimen;
import com.res.hms.lab.infrastructure.repository.LabOrderRepository;
import com.res.hms.lab.infrastructure.repository.LabOrderTestRepository;
import com.res.hms.lab.infrastructure.repository.LabPackageRepository;
import com.res.hms.lab.infrastructure.repository.LabPackageTestRepository;
import com.res.hms.lab.infrastructure.repository.LabSampleRepository;
import com.res.hms.lab.infrastructure.repository.LabSpecimenTypeRepository;
import com.res.hms.lab.infrastructure.repository.LabTestRepository;
import com.res.hms.lab.infrastructure.repository.LabTestSpecimenRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabOrderService {

  private final LabOrderRepository orderRepository;
  private final LabOrderTestRepository orderTestRepository;
  private final LabSampleRepository sampleRepository;
  private final PatientRepository patientRepository;
  private final LabTestRepository testRepository;
  private final LabPackageRepository packageRepository;
  private final LabTestSpecimenRepository testSpecimenRepository;
  private final LabPackageTestRepository packageTestRepository;
  private final LabSpecimenTypeRepository specimenTypeRepository;

  private final LabOrderValidationService validationService;
  private final LabOrderStatusService statusService;
  private final LabOrderCreationService creationService;
  private final LabOrderResultService resultService;

  private final HospitalContext hospitalContext;
  private final JdbcTemplate jdbcTemplate;
  private final LabMapper mapper;

  @Transactional(readOnly = true)
  public List<LabTestParameterDTO> getParametersForOrderTest(UUID orderTestId) {
    return resultService.getParametersForOrderTest(orderTestId);
  }

  @Transactional(readOnly = true)
  public PageResponse<LabQueueItemDTO> getLabQueue(String status, int page, int size) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Pageable pageable = PageRequest.of(page, size);

    Page<LabQueueItemDTO> queue =
        orderTestRepository.findQueueByHospitalAndStatus(hospitalId, status, pageable);
    return new PageResponse<>(
        queue.getContent(),
        queue.getNumber(),
        queue.getSize(),
        queue.getTotalElements(),
        queue.getTotalPages());
  }

  @Transactional
  public String collectSample(UUID orderTestId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    UUID userId = hospitalContext.requireUserId();

    LabOrderTest orderTest =
        orderTestRepository
            .findById(orderTestId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Order test not found", HttpStatus.NOT_FOUND));

    validationService.validateSampleCollection(orderTest);

    String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    Long seq = jdbcTemplate.queryForObject("SELECT nextval('lab.sample_seq')", Long.class);
    String sampleNumber = String.format("SMP-%s-%05d", datePart, seq);

    UUID specimenTypeId = resolveSpecimenType(orderTest);
    saveSample(hospitalId, orderTestId, sampleNumber, specimenTypeId, userId);

    orderTest.setStatus("SAMPLE_COLLECTED");
    orderTestRepository.save(orderTest);
    statusService.syncPrescriptionStatus(orderTestId, PrescriptionLabItemStatus.IN_PROGRESS);

    return sampleNumber;
  }

  private void saveSample(
      UUID hospitalId, UUID orderTestId, String sampleNumber, UUID specimenTypeId, UUID userId) {
    LabSample sample = new LabSample();
    sample.setHospitalId(hospitalId);
    sample.setOrderTestId(orderTestId);
    sample.setSampleNumber(sampleNumber);
    sample.setSpecimenTypeId(specimenTypeId);
    sample.setCollectedBy(userId);
    sample.setCollectedAt(Instant.now());
    sample.setStatus("COLLECTED");
    sampleRepository.save(sample);
  }

  private UUID resolveSpecimenType(LabOrderTest orderTest) {
    UUID specimenTypeId = null;
    if (orderTest.getTestId() != null) {
      specimenTypeId =
          testSpecimenRepository.findByTestId(orderTest.getTestId()).stream()
              .findFirst()
              .map(LabTestSpecimen::getSpecimenTypeId)
              .orElse(null);
    } else if (orderTest.getPackageId() != null) {
      List<LabPackageTest> packageTests =
          packageTestRepository.findByPackageId(orderTest.getPackageId());
      if (!packageTests.isEmpty()) {
        specimenTypeId =
            testSpecimenRepository.findByTestId(packageTests.get(0).getTestId()).stream()
                .findFirst()
                .map(LabTestSpecimen::getSpecimenTypeId)
                .orElse(null);
      }
    }

    if (specimenTypeId == null) {
      specimenTypeId =
          specimenTypeRepository.findAll().stream()
              .findFirst()
              .map(LabSpecimenType::getId)
              .orElseThrow(
                  () ->
                      new ApiException(
                          "CONFIG_ERROR",
                          "No specimen types configured",
                          HttpStatus.INTERNAL_SERVER_ERROR));
    }
    return specimenTypeId;
  }

  @Transactional
  public void markProcessing(UUID orderTestId) {
    statusService.markProcessing(orderTestId);
  }

  @Transactional
  public LabOrder createOrder(LabOrderCreateRequest request) {
    return creationService.createOrder(request);
  }

  @Transactional
  public LabOrder createWalkInOrder(WalkInLabOrderRequest request) {
    return creationService.createWalkInOrder(request);
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
    resultService.saveResults(request);
  }

  @Transactional
  public void verifyResult(UUID orderTestId, String remarks, String status) {
    statusService.verifyResult(orderTestId, remarks, status);
  }

  @Transactional
  public void collectPackageSamples(UUID orderId, UUID packageId) {
    List<LabOrderTest> components =
        orderTestRepository.findAll().stream()
            .filter(ot -> orderId.equals(ot.getOrderId()) && packageId.equals(ot.getPackageId()))
            .collect(Collectors.toList());

    if (components.isEmpty()) {
      throw new ApiException(
          "NOT_FOUND", "No components found for this package in the order", HttpStatus.NOT_FOUND);
    }

    for (LabOrderTest component : components) {
      if ("PENDING".equals(component.getStatus())) {
        collectSample(component.getId());
      }
    }
  }

  @Transactional
  public void markPackageProcessing(UUID orderId, UUID packageId) {
    List<LabOrderTest> components =
        orderTestRepository.findAll().stream()
            .filter(ot -> orderId.equals(ot.getOrderId()) && packageId.equals(ot.getPackageId()))
            .collect(Collectors.toList());

    if (components.isEmpty()) {
      throw new ApiException(
          "NOT_FOUND", "No components found for this package in the order", HttpStatus.NOT_FOUND);
    }

    for (LabOrderTest component : components) {
      if ("SAMPLE_COLLECTED".equals(component.getStatus())) {
        statusService.markProcessing(component.getId());
      }
    }
  }

  @Transactional
  public void verifyPackageResults(UUID orderId, UUID packageId, String remarks, String status) {
    List<LabOrderTest> components =
        orderTestRepository.findAll().stream()
            .filter(ot -> orderId.equals(ot.getOrderId()) && packageId.equals(ot.getPackageId()))
            .collect(Collectors.toList());

    if (components.isEmpty()) {
      throw new ApiException(
          "NOT_FOUND", "No components found for this package in the order", HttpStatus.NOT_FOUND);
    }

    for (LabOrderTest component : components) {
      if ("RESULT_ENTERED".equals(component.getStatus())) {
        statusService.verifyResult(component.getId(), remarks, status);
      }
    }
  }

  @Transactional
  public void lockOrderTest(UUID orderTestId) {
    statusService.lockOrderTest(orderTestId);
  }

  @Transactional
  public void unlockOrderTest(UUID orderTestId, String reason) {
    statusService.unlockOrderTest(orderTestId, reason);
  }

  @Transactional
  public void lockPackageTests(UUID orderId, UUID packageId) {
    statusService.lockPackageTests(orderId, packageId);
  }

  @Transactional
  public void unlockPackageTests(UUID orderId, UUID packageId, String reason) {
    statusService.unlockPackageTests(orderId, packageId, reason);
  }

  @Transactional(readOnly = true)
  public LabPackageResultBundleDTO getPackageResultBundle(UUID orderId, UUID packageId) {
    return resultService.getPackageResultBundle(orderId, packageId);
  }

  @Transactional
  public void savePackageResults(LabPackageResultSaveRequest request) {
    resultService.savePackageResults(request);
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

    return mapper.toResponseDTO(order, patientName, orderTests, testRepository, packageRepository);
  }
}
