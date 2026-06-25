package com.res.hms.lab.application.service;

import com.res.hms.clinical.domain.Prescription;
import com.res.hms.clinical.domain.PrescriptionLabItem;
import com.res.hms.clinical.domain.PrescriptionLabItemStatus;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.finance.application.dto.RecordPaymentRequest;
import com.res.hms.finance.domain.FinanceSourceType;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceItemCategory;
import com.res.hms.lab.application.dto.LabOrderFromRecommendationRequest;
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
public class LabRecommendationService {

  private final LabOrderRepository orderRepository;
  private final LabOrderTestRepository orderTestRepository;
  private final LabTestRepository testRepository;
  private final com.res.hms.clinical.application.prescription.repository.PrescriptionRepository
      prescriptionRepository;
  private final com.res.hms.finance.application.service.InvoiceService invoiceService;
  private final com.res.hms.finance.application.service.PaymentService paymentService;
  private final LabPriceRepository priceRepository;
  private final HospitalContext hospitalContext;
  private final LabPackageRepository packageRepository;
  private final LabPackageTestRepository packageTestRepository;
  private final LabHospitalPackageConfigRepository packageConfigRepository;

  @Transactional
  public LabOrder createOrderFromRecommendation(LabOrderFromRecommendationRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    UUID userId = hospitalContext.requireUserId();

    Prescription prescription =
        prescriptionRepository
            .findByIdAndHospitalId(request.getPrescriptionId(), hospitalId)
            .orElseThrow(
                () ->
                    new ApiException("NOT_FOUND", "Prescription not found", HttpStatus.NOT_FOUND));

    boolean hasTests = request.getTestIds() != null && !request.getTestIds().isEmpty();
    boolean hasPackages = request.getPackageIds() != null && !request.getPackageIds().isEmpty();

    if (!hasTests && !hasPackages) {
      throw new ApiException(
          "BAD_REQUEST", "No tests or packages selected", HttpStatus.BAD_REQUEST);
    }

    UUID visitId =
        prescription.getAppointment() != null ? prescription.getAppointment().getVisitId() : null;
    LabOrder order = createLabOrderRecord(hospitalId, prescription.getPatientId(), visitId);

    Invoice invoice =
        invoiceService.getOrCreateInvoice(hospitalId, prescription.getPatientId(), visitId);

    if (hasTests) {
      processRecommendedTests(
          hospitalId, prescription, order.getId(), invoice, request.getTestIds());
    }
    if (hasPackages) {
      processRecommendedPackages(
          hospitalId, prescription, order.getId(), invoice, request.getPackageIds());
    }

    processRecommendationPayment(hospitalId, prescription, visitId, userId, request);

    prescriptionRepository.save(prescription);
    return order;
  }

  private LabOrder createLabOrderRecord(UUID hospitalId, UUID patientId, UUID visitId) {
    LabOrder order =
        LabOrder.builder()
            .hospitalId(hospitalId)
            .patientId(patientId)
            .encounterId(visitId)
            .orderNumber("ORD-" + System.currentTimeMillis())
            .status("ORDERED")
            .orderedAt(Instant.now())
            .build();
    return orderRepository.save(order);
  }

  private void processRecommendedTests(
      UUID hospitalId,
      Prescription prescription,
      UUID orderId,
      Invoice invoice,
      List<UUID> testIds) {
    for (UUID testId : testIds) {
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

      String testName =
          prescription.getLabItems().stream()
              .filter(item -> item.getTestId().equals(testId))
              .findFirst()
              .map(PrescriptionLabItem::getTestName)
              .orElse(test.getTestName());

      invoiceService.addItem(
          invoice.getId(),
          testName,
          InvoiceItemCategory.LAB,
          BigDecimal.ONE,
          testPrice,
          BigDecimal.ZERO,
          FinanceSourceType.LAB_ORDER,
          orderTest.getId(),
          prescription.getDoctorId(),
          prescription.getDoctorName(),
          null);

      updatePrescriptionLabItemStatus(prescription, testId, orderTest.getId());
    }
  }

  private void updatePrescriptionLabItemStatus(
      Prescription prescription, UUID testId, UUID orderTestId) {
    prescription.getLabItems().stream()
        .filter(item -> item.getTestId() != null && item.getTestId().equals(testId))
        .filter(item -> item.getStatus() == PrescriptionLabItemStatus.PENDING)
        .forEach(
            item -> {
              item.setStatus(PrescriptionLabItemStatus.ORDERED);
              item.setLabOrderTestId(orderTestId);
            });
  }

  private void processRecommendedPackages(
      UUID hospitalId,
      Prescription prescription,
      UUID orderId,
      Invoice invoice,
      List<UUID> packageIds) {
    for (UUID packageId : packageIds) {
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

      String packageName =
          prescription.getLabItems().stream()
              .filter(item -> packageId.equals(item.getPackageId()))
              .findFirst()
              .map(PrescriptionLabItem::getPackageName)
              .orElse(labPackage.getPackageName());

      invoiceService.addItem(
          invoice.getId(),
          packageName,
          InvoiceItemCategory.LAB,
          BigDecimal.ONE,
          packagePrice,
          BigDecimal.ZERO,
          FinanceSourceType.LAB_ORDER,
          firstOrderTestId,
          prescription.getDoctorId(),
          prescription.getDoctorName(),
          null);

      updatePrescriptionLabPackageStatus(prescription, packageId, firstOrderTestId);
    }
  }

  private void updatePrescriptionLabPackageStatus(
      Prescription prescription, UUID packageId, UUID orderTestId) {
    prescription.getLabItems().stream()
        .filter(item -> packageId.equals(item.getPackageId()))
        .filter(item -> item.getStatus() == PrescriptionLabItemStatus.PENDING)
        .forEach(
            item -> {
              item.setStatus(PrescriptionLabItemStatus.ORDERED);
              item.setLabOrderTestId(orderTestId);
            });
  }

  private void processRecommendationPayment(
      UUID hospitalId,
      Prescription prescription,
      UUID visitId,
      UUID userId,
      LabOrderFromRecommendationRequest request) {
    if (request.getAmountPaid() != null && request.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
      RecordPaymentRequest paymentRequest = new RecordPaymentRequest();
      paymentRequest.setPatientId(prescription.getPatientId());
      paymentRequest.setVisitId(visitId);
      paymentRequest.setAmount(request.getAmountPaid());
      paymentRequest.setPaymentMode(request.getPaymentMode());
      paymentRequest.setReferenceNumber(request.getReferenceNumber());
      Invoice invoice =
          invoiceService.getOrCreateInvoice(hospitalId, prescription.getPatientId(), visitId);
      paymentRequest.setInvoiceId(invoice.getId());

      paymentService.recordPayment(hospitalId, paymentRequest, userId);
    }
  }
}
