package com.res.hms.lab.application.service;

import com.res.hms.clinical.domain.PrescriptionLabItemStatus;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.exception.ApiException;
import com.res.hms.lab.domain.LabOrder;
import com.res.hms.lab.domain.LabOrderTest;
import com.res.hms.lab.domain.LabResult;
import com.res.hms.lab.domain.TestCategory;
import com.res.hms.lab.infrastructure.repository.LabOrderRepository;
import com.res.hms.lab.infrastructure.repository.LabOrderTestRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabOrderStatusService {

  private final LabOrderRepository orderRepository;
  private final LabOrderTestRepository orderTestRepository;
  private final LabResultService labResultService;
  private final com.res.hms.clinical.infrastructure.repository.PrescriptionLabItemRepository
      prescriptionLabItemRepository;
  private final com.res.hms.clinical.application.prescription.repository.PrescriptionRepository
      prescriptionRepository;
  private final AppointmentRepository appointmentRepository;

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

  public void updateOrderStatusIfAllTestsDone(UUID orderId) {
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

  public void syncPrescriptionStatus(UUID orderTestId, PrescriptionLabItemStatus status) {
    prescriptionLabItemRepository
        .findByLabOrderTestId(orderTestId)
        .ifPresentOrElse(
            item -> {
              item.setStatus(status);
              prescriptionLabItemRepository.save(item);
            },
            () -> {
              orderTestRepository
                  .findById(orderTestId)
                  .ifPresent(
                      orderTest -> {
                        if (orderTest.getPackageId() != null) {
                          orderRepository
                              .findById(orderTest.getOrderId())
                              .ifPresent(
                                  order -> {
                                    if (order.getEncounterId() != null) {
                                      appointmentRepository
                                          .findByVisitId(order.getEncounterId())
                                          .ifPresent(
                                              app -> {
                                                prescriptionRepository
                                                    .findByAppointmentId(app.getId())
                                                    .ifPresent(
                                                        prescription -> {
                                                          prescription.getLabItems().stream()
                                                              .filter(
                                                                  item ->
                                                                      orderTest
                                                                          .getPackageId()
                                                                          .equals(
                                                                              item.getPackageId()))
                                                              .forEach(
                                                                  item -> {
                                                                    if (status
                                                                        == PrescriptionLabItemStatus
                                                                            .COMPLETED) {
                                                                      boolean
                                                                          allPackageTestsVerified =
                                                                              orderTestRepository
                                                                                  .findAll()
                                                                                  .stream()
                                                                                  .filter(
                                                                                      ot ->
                                                                                          order
                                                                                                  .getId()
                                                                                                  .equals(
                                                                                                      ot
                                                                                                          .getOrderId())
                                                                                              && orderTest
                                                                                                  .getPackageId()
                                                                                                  .equals(
                                                                                                      ot
                                                                                                          .getPackageId()))
                                                                                  .allMatch(
                                                                                      ot ->
                                                                                          "VERIFIED"
                                                                                              .equals(
                                                                                                  ot
                                                                                                      .getStatus()));
                                                                      if (allPackageTestsVerified) {
                                                                        item.setStatus(
                                                                            PrescriptionLabItemStatus
                                                                                .COMPLETED);
                                                                        prescriptionLabItemRepository
                                                                            .save(item);
                                                                      }
                                                                    } else {
                                                                      item.setStatus(status);
                                                                      prescriptionLabItemRepository
                                                                          .save(item);
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
  public void lockOrderTest(UUID orderTestId) {
    LabOrderTest orderTest =
        orderTestRepository
            .findById(orderTestId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Order test not found", HttpStatus.NOT_FOUND));
    if (!"VERIFIED".equals(orderTest.getStatus())) {
      throw new ApiException(
          "INVALID_STATE", "Only verified tests can be locked", HttpStatus.BAD_REQUEST);
    }
    orderTest.setLocked(true);
    orderTestRepository.save(orderTest);
  }

  @Transactional
  public void unlockOrderTest(UUID orderTestId, String reason) {
    LabOrderTest orderTest =
        orderTestRepository
            .findById(orderTestId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Order test not found", HttpStatus.NOT_FOUND));
    orderTest.setLocked(false);
    orderTest.setLockedReason(reason);
    orderTestRepository.save(orderTest);
  }

  @Transactional
  public void lockPackageTests(UUID orderId, UUID packageId) {
    List<LabOrderTest> components =
        orderTestRepository.findAll().stream()
            .filter(ot -> orderId.equals(ot.getOrderId()) && packageId.equals(ot.getPackageId()))
            .collect(Collectors.toList());
    if (components.isEmpty()) {
      throw new ApiException("NOT_FOUND", "No tests found for this package", HttpStatus.NOT_FOUND);
    }
    boolean allVerified = components.stream().allMatch(c -> "VERIFIED".equals(c.getStatus()));
    if (!allVerified) {
      throw new ApiException(
          "INVALID_STATE",
          "All package tests must be verified before locking",
          HttpStatus.BAD_REQUEST);
    }
    for (LabOrderTest test : components) {
      test.setLocked(true);
      orderTestRepository.save(test);
    }
  }

  @Transactional
  public void unlockPackageTests(UUID orderId, UUID packageId, String reason) {
    List<LabOrderTest> components =
        orderTestRepository.findAll().stream()
            .filter(ot -> orderId.equals(ot.getOrderId()) && packageId.equals(ot.getPackageId()))
            .collect(Collectors.toList());
    for (LabOrderTest test : components) {
      test.setLocked(false);
      test.setLockedReason(reason);
      orderTestRepository.save(test);
    }
  }
}
