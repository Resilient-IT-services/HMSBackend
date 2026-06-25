package com.res.hms.lab.application.service;

import com.res.hms.clinical.domain.Patient;
import com.res.hms.clinical.infrastructure.repository.PatientRepository;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.finance.application.service.InvoiceService;
import com.res.hms.finance.domain.FinanceSourceType;
import com.res.hms.finance.domain.InvoiceItemCategory;
import com.res.hms.lab.application.dto.LabOrderCreateRequest;
import com.res.hms.lab.application.dto.WalkInLabOrderRequest;
import com.res.hms.lab.domain.LabHospitalPackageConfig;
import com.res.hms.lab.domain.LabOrder;
import com.res.hms.lab.domain.LabOrderTest;
import com.res.hms.lab.domain.LabPackage;
import com.res.hms.lab.domain.LabPackageTest;
import com.res.hms.lab.domain.LabPrice;
import com.res.hms.lab.domain.LabTest;
import com.res.hms.lab.infrastructure.repository.LabHospitalPackageConfigRepository;
import com.res.hms.lab.infrastructure.repository.LabOrderRepository;
import com.res.hms.lab.infrastructure.repository.LabOrderTestRepository;
import com.res.hms.lab.infrastructure.repository.LabPackageRepository;
import com.res.hms.lab.infrastructure.repository.LabPackageTestRepository;
import com.res.hms.lab.infrastructure.repository.LabPriceRepository;
import com.res.hms.lab.infrastructure.repository.LabTestRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabOrderCreationService {

  private final LabOrderRepository orderRepository;
  private final LabOrderTestRepository orderTestRepository;
  private final LabTestRepository testRepository;
  private final PatientRepository patientRepository;
  private final LabPackageRepository packageRepository;
  private final LabPackageTestRepository packageTestRepository;
  private final LabPriceRepository priceRepository;
  private final LabHospitalPackageConfigRepository packageConfigRepository;
  private final InvoiceService invoiceService;
  private final HospitalContext hospitalContext;
  private final LabOrderValidationService validationService;

  @Transactional
  public LabOrder createOrder(LabOrderCreateRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    validationService.validateNoDuplicates(request.getTestIds(), request.getPackageIds());

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

  @Transactional
  public LabOrder createWalkInOrder(WalkInLabOrderRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    validationService.validateNoDuplicates(request.getTestIds(), request.getPackageIds());

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
            hospitalId, patient.getId(), null, request.getWalkInName(), request.getWalkInPhone());

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
                    () ->
                        new ApiException(
                            "NOT_FOUND", "Test not found: " + testId, HttpStatus.NOT_FOUND));

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
                    () ->
                        new ApiException(
                            "NOT_FOUND", "Package not found: " + packageId, HttpStatus.NOT_FOUND));

        List<LabPackageTest> components = packageTestRepository.findByPackageId(packageId);
        UUID firstOrderTestId = null;

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
}
