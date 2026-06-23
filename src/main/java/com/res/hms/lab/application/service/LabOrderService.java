package com.res.hms.lab.application.service;

import com.res.hms.clinical.domain.Patient;
import com.res.hms.clinical.domain.Appointment;
import com.res.hms.clinical.domain.Prescription;
import com.res.hms.clinical.domain.PrescriptionLabItemStatus;
import com.res.hms.clinical.infrastructure.repository.PatientRepository;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.lab.application.dto.LabOrderCreateRequest;
import com.res.hms.lab.application.dto.LabOrderResponseDTO;
import com.res.hms.lab.application.dto.LabQueueItemDTO;
import com.res.hms.lab.application.dto.LabResultSaveRequest;
import com.res.hms.lab.application.dto.LabTestParameterDTO;
import com.res.hms.lab.application.dto.WalkInLabOrderRequest;
import com.res.hms.finance.domain.FinanceSourceType;
import com.res.hms.finance.domain.InvoiceItemCategory;
import com.res.hms.lab.domain.LabHospitalPackageConfig;
import com.res.hms.lab.domain.LabOrder;
import com.res.hms.lab.domain.LabOrderTest;
import com.res.hms.lab.domain.LabPackageTest;
import com.res.hms.lab.domain.LabPackage;
import com.res.hms.lab.application.dto.LabPackageResultBundleDTO;
import com.res.hms.lab.application.dto.LabPackageResultSaveRequest;
import com.res.hms.lab.domain.LabPrice;
import com.res.hms.lab.domain.LabResult;
import com.res.hms.lab.domain.LabSample;
import com.res.hms.lab.domain.LabSpecimenType;
import com.res.hms.lab.domain.LabTest;
import com.res.hms.lab.domain.LabTestSpecimen;
import com.res.hms.lab.domain.TestCategory;
import com.res.hms.lab.infrastructure.repository.LabHospitalPackageConfigRepository;
import com.res.hms.lab.infrastructure.repository.LabOrderRepository;
import com.res.hms.lab.infrastructure.repository.LabOrderTestRepository;
import com.res.hms.lab.infrastructure.repository.LabPackageRepository;
import com.res.hms.lab.infrastructure.repository.LabPackageTestRepository;
import com.res.hms.lab.infrastructure.repository.LabResultParameterRepository;
import com.res.hms.lab.infrastructure.repository.LabSampleRepository;
import com.res.hms.lab.infrastructure.repository.LabTestParameterRepository;
import com.res.hms.lab.infrastructure.repository.LabTestRepository;
import com.res.hms.lab.infrastructure.repository.LabTestSpecimenRepository;
import java.math.BigDecimal;
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
  private final LabTestRepository testRepository;
  private final LabPackageRepository packageRepository;
  private final PatientRepository patientRepository;
  private final AppointmentRepository appointmentRepository;
  private final LabSampleRepository sampleRepository;
  private final LabResultParameterRepository resultParameterRepository;
  private final LabTestParameterRepository testParameterRepository;
  private final com.res.hms.clinical.infrastructure.repository.PrescriptionLabItemRepository
      prescriptionLabItemRepository;
  private final com.res.hms.clinical.application.prescription.repository.PrescriptionRepository
      prescriptionRepository;
  private final com.res.hms.finance.application.service.InvoiceService invoiceService;
  private final com.res.hms.finance.application.service.PaymentService paymentService;
  private final com.res.hms.lab.infrastructure.repository.LabPriceRepository priceRepository;
  private final LabHospitalPackageConfigRepository packageConfigRepository;
  private final LabTestSpecimenRepository testSpecimenRepository;
  private final LabPackageTestRepository packageTestRepository;
  private final com.res.hms.lab.infrastructure.repository.LabSpecimenTypeRepository
      specimenTypeRepository;
  private final LabResultService labResultService;
  private final HospitalContext hospitalContext;
  private final JdbcTemplate jdbcTemplate;
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

    validateSampleCollection(orderTest);

    String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    Long seq = jdbcTemplate.queryForObject("SELECT nextval('lab.sample_seq')", Long.class);
    String sampleNumber = String.format("SMP-%s-%05d", datePart, seq);

    UUID specimenTypeId = resolveSpecimenType(orderTest);
    saveSample(hospitalId, orderTestId, sampleNumber, specimenTypeId, userId);

    orderTest.setStatus("SAMPLE_COLLECTED");
    orderTestRepository.save(orderTest);
    syncPrescriptionStatus(orderTestId, PrescriptionLabItemStatus.IN_PROGRESS);

    return sampleNumber;
  }

  private void validateSampleCollection(LabOrderTest orderTest) {
    if (orderTest.getCategory() != null && orderTest.getCategory() != TestCategory.LAB) {
      throw new ApiException(
          "INVALID_CATEGORY",
          "Sample collection is only allowed for LAB tests",
          HttpStatus.BAD_REQUEST);
    }

    if (!"PENDING".equals(orderTest.getStatus())) {
      throw new ApiException(
          "INVALID_STATE", "Sample already collected or processed", HttpStatus.BAD_REQUEST);
    }
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
    LabOrderTest orderTest =
        orderTestRepository
            .findById(orderTestId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Order test not found", HttpStatus.NOT_FOUND));

    if (!"SAMPLE_COLLECTED".equals(orderTest.getStatus())) {
      throw new ApiException(
          "INVALID_STATE", "Invalid status: " + orderTest.getStatus(), HttpStatus.BAD_REQUEST);
    }

    orderTest.setStatus("PROCESSING");
    orderTestRepository.save(orderTest);
    syncPrescriptionStatus(orderTestId, PrescriptionLabItemStatus.IN_PROGRESS);
  }

  @Transactional
  public LabOrder createOrder(LabOrderCreateRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();validateNoDuplicates(request.getTestIds(), request.getPackageIds());

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
    processOrderTests(order.getId(), request.getTestIds(), request.getPackageIds());
    return order;
  }

  private void validateNoDuplicates(List<UUID> testIds, List<UUID> packageIds) {
    if (testIds == null || testIds.isEmpty() || packageIds == null || packageIds.isEmpty()) return;

    for (UUID packageId : packageIds) {
      List<UUID> coveredTestIds =
          packageTestRepository.findByPackageId(packageId).stream()
              .map(LabPackageTest::getTestId)
              .collect(Collectors.toList());

      List<String> duplicateNames =
          testIds.stream()
              .filter(coveredTestIds::contains)
              .map(
                  testId ->
                      testRepository
                          .findById(testId)
                          .map(LabTest::getTestName)
                          .orElse(testId.toString()))
              .collect(Collectors.toList());

      if (!duplicateNames.isEmpty()) {
        String packageName =
            packageRepository
                .findById(packageId)
                .map(LabPackage::getPackageName)
                .orElse(packageId.toString());
        throw new ApiException(
            "DUPLICATE_TEST",
            String.format(
                "%s %s already included in \"%s\". Remove the duplicate test(s) before ordering.",
                String.join(", ", duplicateNames),
                duplicateNames.size() == 1 ? "is" : "are",
                packageName),
            HttpStatus.BAD_REQUEST);
      }
    }
  }

  @Transactional
  public LabOrder createWalkInOrder(WalkInLabOrderRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    validateNoDuplicates(request.getTestIds(), request.getPackageIds());

    Patient patient = new Patient();
    patient.setHospitalId(hospitalId);
    patient.setFullName(request.getWalkInName());
    patient.setPhone(request.getWalkInPhone());
    patient.setAddress(request.getWalkInAddress());
    patient.setPatientCode("WI-" + System.currentTimeMillis());
    patient.setDeleted(false);
    patient = patientRepository.save(patient);

    LabOrder order =
        LabOrder.builder()
            .hospitalId(hospitalId)
            .patientId(patient.getId())
            .orderSource("WALK_IN")
            .orderNumber("WI-" + System.currentTimeMillis())
            .status("ORDERED")
            .orderedAt(Instant.now())
            .build();

    order = orderRepository.save(order);

    com.res.hms.finance.domain.Invoice invoice =
        invoiceService.getOrCreateInvoice(
            hospitalId, patient.getId(), null,
            request.getWalkInName(), request.getWalkInPhone());

    createWalkInInvoiceItems(hospitalId, order.getId(), invoice.getId(), request);

    return order;
  }

  private void createWalkInInvoiceItems(
      UUID hospitalId, UUID orderId, UUID invoiceId, WalkInLabOrderRequest request) {

    if (request.getTestIds() != null) {
      for (UUID testId : request.getTestIds()) {
        LabTest test =
            testRepository
                .findById(testId)
                .orElseThrow(
                    () -> new ApiException("NOT_FOUND", "Test not found: " + testId, HttpStatus.NOT_FOUND));

        LabOrderTest orderTest =
            orderTestRepository.save(
                LabOrderTest.builder()
                    .orderId(orderId)
                    .testId(testId)
                    .testName(test.getTestName())
                    .category(test.getCategory())
                    .resultEntryType(test.getResultEntryType())
                    .status("PENDING")
                    .build());

        BigDecimal testPrice =
            priceRepository
                .findByHospitalIdAndTestId(hospitalId, testId)
                .map(LabPrice::getStandardPrice)
                .orElse(BigDecimal.ZERO);

        invoiceService.addItem(
            invoiceId,
            test.getTestName(),
            InvoiceItemCategory.LAB,
            BigDecimal.ONE,
            testPrice,
            BigDecimal.ZERO,
            FinanceSourceType.LAB_ORDER,
            orderTest.getId(),
            null,
            null,
            null);
      }
    }

    if (request.getPackageIds() != null) {
      for (UUID packageId : request.getPackageIds()) {
        LabPackage labPackage =
            packageRepository
                .findById(packageId)
                .orElseThrow(
                    () -> new ApiException("NOT_FOUND", "Package not found: " + packageId, HttpStatus.NOT_FOUND));

        List<LabPackageTest> components = packageTestRepository.findByPackageId(packageId);
        UUID firstOrderTestId = null;

        for (LabPackageTest component : components) {
          LabTest test =
              testRepository
                  .findById(component.getTestId())
                  .orElseThrow(
                      () -> new ApiException("NOT_FOUND", "Test not found in package: " + component.getTestId(), HttpStatus.NOT_FOUND));

          LabOrderTest orderTest =
              orderTestRepository.save(
                  LabOrderTest.builder()
                      .orderId(orderId)
                      .testId(test.getId())
                      .testName(test.getTestName())
                      .category(test.getCategory())
                      .resultEntryType(test.getResultEntryType())
                      .packageId(packageId)
                      .status("PENDING")
                      .build());

          if (firstOrderTestId == null) {
            firstOrderTestId = orderTest.getId();
          }
        }

        BigDecimal packagePrice =
            packageConfigRepository
                .findByHospitalIdAndPackageId(hospitalId, packageId)
                .map(LabHospitalPackageConfig::getStandardPrice)
                .orElse(BigDecimal.ZERO);

        invoiceService.addItem(
            invoiceId,
            labPackage.getPackageName(),
            InvoiceItemCategory.LAB,
            BigDecimal.ONE,
            packagePrice,
            BigDecimal.ZERO,
            FinanceSourceType.LAB_ORDER,
            firstOrderTestId,
            null,
            null,
            null);
      }
    }
  }

  private void processOrderTests(UUID orderId, List<UUID> testIds, List<UUID> packageIds) {
    if (testIds != null) {
      for (UUID testId : testIds) {
        LabTest test =
            testRepository
                .findById(testId)
                .orElseThrow(
                    () ->
                        new ApiException(
                            "NOT_FOUND", "Test not found: " + testId, HttpStatus.NOT_FOUND));

        orderTestRepository.save(
            LabOrderTest.builder()
                .orderId(orderId)
                .testId(testId)
                .testName(test.getTestName())
                .category(test.getCategory())
                .resultEntryType(test.getResultEntryType())
                .status("PENDING")
                .build());
      }
    }

    if (packageIds != null) {
      for (UUID packageId : packageIds) {
        List<LabPackageTest> components = packageTestRepository.findByPackageId(packageId);
        for (LabPackageTest component : components) {
          LabTest test =
              testRepository
                  .findById(component.getTestId())
                  .orElseThrow(
                      () ->
                          new ApiException(
                              "NOT_FOUND",
                              "Test not found in package: " + component.getTestId(),
                              HttpStatus.NOT_FOUND));
          orderTestRepository.save(
              LabOrderTest.builder()
                  .orderId(orderId)
                  .testId(test.getId())
                  .testName(test.getTestName())
                  .category(test.getCategory())
                  .resultEntryType(test.getResultEntryType())
                  .packageId(packageId)
                  .status("PENDING")
                  .build());
        }
      }
    }
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

    LabResult result = labResultService.saveOrUpdateLabResult(orderTest, request.getFindings());
    labResultService.processResultParameters(result.getId(), request.getResults());

    orderTest.setStatus("RESULT_ENTERED");
    orderTestRepository.save(orderTest);
    syncPrescriptionStatus(request.getOrderTestId(), PrescriptionLabItemStatus.IN_PROGRESS);
  }

  private void syncPrescriptionStatus(UUID orderTestId, PrescriptionLabItemStatus status) {
    prescriptionLabItemRepository
        .findByLabOrderTestId(orderTestId)
        .ifPresentOrElse(
            item -> {
              item.setStatus(status);
              prescriptionLabItemRepository.save(item);
            },
            () -> {
              orderTestRepository.findById(orderTestId).ifPresent(orderTest -> {
                if (orderTest.getPackageId() != null) {
                  orderRepository.findById(orderTest.getOrderId()).ifPresent(order -> {
                    if (order.getEncounterId() != null) {
                      appointmentRepository.findByVisitId(order.getEncounterId()).ifPresent(app -> {
                        prescriptionRepository.findByAppointmentId(app.getId()).ifPresent(prescription -> {
                          prescription.getLabItems().stream()
                              .filter(item -> orderTest.getPackageId().equals(item.getPackageId()))
                              .forEach(item -> {
                                if (status == PrescriptionLabItemStatus.COMPLETED) {
                                  boolean allPackageTestsVerified = orderTestRepository.findAll().stream()
                                      .filter(ot -> order.getId().equals(ot.getOrderId()) && orderTest.getPackageId().equals(ot.getPackageId()))
                                      .allMatch(ot -> "VERIFIED".equals(ot.getStatus()));
                                  if (allPackageTestsVerified) {
                                    item.setStatus(PrescriptionLabItemStatus.COMPLETED);
                                    prescriptionLabItemRepository.save(item);
                                  }
                                } else {
                                  item.setStatus(status);
                                  prescriptionLabItemRepository.save(item);
                                }
                              });
                        });
                      });
                    }
                  });
                }
              });
            });
  }

  @Transactional
  public void verifyResult(UUID orderTestId, String remarks, String status) {
    LabOrderTest orderTest =
        orderTestRepository
            .findById(orderTestId)
            .orElseThrow(() -> new RuntimeException("Order test not found"));

    if (!"RESULT_ENTERED".equals(orderTest.getStatus())) {
      throw new RuntimeException("Test not in RESULT_ENTERED status");
    }

    LabResult result = labResultService.findResultByOrderTestId(orderTestId);
    if (result == null) {
      throw new RuntimeException("Result not found");
    }

    labResultService.saveVerification(result.getId(), status, remarks);
    handleVerificationOutcome(orderTest, status);
  }

  private void handleVerificationOutcome(LabOrderTest orderTest, String status) {
    if ("VERIFIED".equalsIgnoreCase(status)) {
      orderTest.setStatus("VERIFIED");
      orderTestRepository.save(orderTest);
      syncPrescriptionStatus(orderTest.getId(), PrescriptionLabItemStatus.COMPLETED);
      updateOrderStatusIfAllTestsDone(orderTest.getOrderId());
    } else if ("REJECTED".equalsIgnoreCase(status)) {
      orderTest.setStatus(
          orderTest.getCategory() == TestCategory.LAB ? "SAMPLE_COLLECTED" : "PENDING");
      orderTestRepository.save(orderTest);
    }
  }

  private void updateOrderStatusIfAllTestsDone(UUID orderId) {
    LabOrder order = orderRepository.findById(orderId).orElse(null);
    if (order != null) {
      boolean allDone =
          orderTestRepository.findAll().stream()
              .filter(ot -> order.getId().equals(ot.getOrderId()))
              .allMatch(ot -> "VERIFIED".equals(ot.getStatus()));

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

    return mapper.toResponseDTO(order, patientName, orderTests, testRepository, packageRepository);
  }

  @Transactional
  public void collectPackageSamples(UUID orderId, UUID packageId) {
    List<LabOrderTest> components = orderTestRepository.findAll().stream()
        .filter(ot -> orderId.equals(ot.getOrderId()) && packageId.equals(ot.getPackageId()))
        .collect(Collectors.toList());

    if (components.isEmpty()) {
      throw new ApiException("NOT_FOUND", "No components found for this package in the order", HttpStatus.NOT_FOUND);
    }

    for (LabOrderTest component : components) {
      if ("PENDING".equals(component.getStatus())) {
        collectSample(component.getId());
      }
    }
  }

  @Transactional
  public void markPackageProcessing(UUID orderId, UUID packageId) {
    List<LabOrderTest> components = orderTestRepository.findAll().stream()
        .filter(ot -> orderId.equals(ot.getOrderId()) && packageId.equals(ot.getPackageId()))
        .collect(Collectors.toList());

    if (components.isEmpty()) {
      throw new ApiException("NOT_FOUND", "No components found for this package in the order", HttpStatus.NOT_FOUND);
    }

    for (LabOrderTest component : components) {
      if ("SAMPLE_COLLECTED".equals(component.getStatus())) {
        markProcessing(component.getId());
      }
    }
  }

  @Transactional
  public void verifyPackageResults(UUID orderId, UUID packageId, String remarks, String status) {
    List<LabOrderTest> components = orderTestRepository.findAll().stream()
        .filter(ot -> orderId.equals(ot.getOrderId()) && packageId.equals(ot.getPackageId()))
        .collect(Collectors.toList());

    if (components.isEmpty()) {
      throw new ApiException("NOT_FOUND", "No components found for this package in the order", HttpStatus.NOT_FOUND);
    }

    for (LabOrderTest component : components) {
      if ("RESULT_ENTERED".equals(component.getStatus())) {
        verifyResult(component.getId(), remarks, status);
      }
    }
  }

  @Transactional
  public void lockOrderTest(UUID orderTestId) {
    LabOrderTest orderTest =
        orderTestRepository
            .findById(orderTestId)
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Order test not found", HttpStatus.NOT_FOUND));
    if (!"VERIFIED".equals(orderTest.getStatus())) {
      throw new ApiException("INVALID_STATE", "Only verified tests can be locked", HttpStatus.BAD_REQUEST);
    }
    orderTest.setLocked(true);
    orderTestRepository.save(orderTest);
  }

  @Transactional
  public void unlockOrderTest(UUID orderTestId, String reason) {
    LabOrderTest orderTest =
        orderTestRepository
            .findById(orderTestId)
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Order test not found", HttpStatus.NOT_FOUND));
    orderTest.setLocked(false);
    orderTest.setLockedReason(reason);
    orderTestRepository.save(orderTest);
  }

  @Transactional
  public void lockPackageTests(UUID orderId, UUID packageId) {
    List<LabOrderTest> components = orderTestRepository.findAll().stream()
        .filter(ot -> orderId.equals(ot.getOrderId()) && packageId.equals(ot.getPackageId()))
        .collect(Collectors.toList());
    if (components.isEmpty()) {
      throw new ApiException("NOT_FOUND", "No tests found for this package", HttpStatus.NOT_FOUND);
    }
    boolean allVerified = components.stream().allMatch(c -> "VERIFIED".equals(c.getStatus()));
    if (!allVerified) {
      throw new ApiException("INVALID_STATE", "All package tests must be verified before locking", HttpStatus.BAD_REQUEST);
    }
    for (LabOrderTest test : components) {
      test.setLocked(true);
      orderTestRepository.save(test);
    }
  }

  @Transactional
  public void unlockPackageTests(UUID orderId, UUID packageId, String reason) {
    List<LabOrderTest> components = orderTestRepository.findAll().stream()
        .filter(ot -> orderId.equals(ot.getOrderId()) && packageId.equals(ot.getPackageId()))
        .collect(Collectors.toList());
    for (LabOrderTest test : components) {
      test.setLocked(false);
      test.setLockedReason(reason);
      orderTestRepository.save(test);
    }
  }

  @Transactional(readOnly = true)
  public LabPackageResultBundleDTO getPackageResultBundle(UUID orderId, UUID packageId) {
    LabPackage labPackage = packageRepository.findById(packageId)
        .orElseThrow(() -> new ApiException("NOT_FOUND", "Package not found", HttpStatus.NOT_FOUND));

    List<LabOrderTest> components = orderTestRepository.findAll().stream()
        .filter(ot -> orderId.equals(ot.getOrderId()) && packageId.equals(ot.getPackageId()))
        .collect(Collectors.toList());

    String status = "PENDING";
    if (!components.isEmpty()) {
      boolean allVerified = components.stream().allMatch(c -> "VERIFIED".equals(c.getStatus()));
      boolean allResultEntered = components.stream().allMatch(c -> "VERIFIED".equals(c.getStatus()) || "RESULT_ENTERED".equals(c.getStatus()));
      boolean anyProcessing = components.stream().anyMatch(c -> "PROCESSING".equals(c.getStatus()));
      boolean anyCollected = components.stream().anyMatch(c -> "SAMPLE_COLLECTED".equals(c.getStatus()));

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

    List<LabPackageResultBundleDTO.PackageTestBundleDTO> testBundles = components.stream().map(comp -> {
      LabResult existingResult = labResultService.findResultByOrderTestId(comp.getId());
      String findings = existingResult != null ? existingResult.getFindings() : null;

      List<LabTestParameterDTO> params = getParametersForOrderTest(comp.getId());

      return LabPackageResultBundleDTO.PackageTestBundleDTO.builder()
          .orderTestId(comp.getId())
          .testId(comp.getTestId())
          .testName(comp.getTestName())
          .category(comp.getCategory() != null ? comp.getCategory().name() : null)
          .resultEntryType(comp.getResultEntryType() != null ? comp.getResultEntryType().name() : null)
          .status(comp.getStatus())
          .findings(findings)
          .parameters(params)
          .build();
    }).collect(Collectors.toList());

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
        LabResultSaveRequest singleRequest = LabResultSaveRequest.builder()
            .orderTestId(testRequest.getOrderTestId())
            .findings(testRequest.getFindings())
            .results(testRequest.getResults())
            .build();
        saveResults(singleRequest);
      }
    }
  }
}
