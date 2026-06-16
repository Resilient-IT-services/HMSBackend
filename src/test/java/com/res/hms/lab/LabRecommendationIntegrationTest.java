package com.res.hms.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.res.hms.clinical.application.prescription.repository.PrescriptionRepository;
import com.res.hms.clinical.domain.Prescription;
import com.res.hms.clinical.domain.PrescriptionLabItem;
import com.res.hms.clinical.domain.PrescriptionLabItemStatus;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.finance.application.dto.RecordPaymentRequest;
import com.res.hms.finance.application.service.InvoiceService;
import com.res.hms.finance.application.service.PaymentService;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.PaymentMode;
import com.res.hms.lab.application.dto.LabOrderFromRecommendationRequest;
import com.res.hms.lab.application.service.LabRecommendationService;
import com.res.hms.lab.domain.LabOrder;
import com.res.hms.lab.domain.LabOrderTest;
import com.res.hms.lab.domain.LabPrice;
import com.res.hms.lab.domain.LabTest;
import com.res.hms.lab.infrastructure.repository.LabOrderRepository;
import com.res.hms.lab.infrastructure.repository.LabOrderTestRepository;
import com.res.hms.lab.infrastructure.repository.LabPriceRepository;
import com.res.hms.lab.infrastructure.repository.LabTestRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LabRecommendationIntegrationTest {

  @Mock private PrescriptionRepository prescriptionRepository;
  @Mock private LabOrderRepository labOrderRepository;
  @Mock private LabOrderTestRepository labOrderTestRepository;
  @Mock private LabPriceRepository labPriceRepository;
  @Mock private LabTestRepository labTestRepository;
  @Mock private InvoiceService invoiceService;
  @Mock private PaymentService paymentService;
  @Mock private HospitalContext hospitalContext;

  @InjectMocks private LabRecommendationService labRecommendationService;

  private UUID hospitalId;
  private UUID userId;
  private UUID patientId;
  private UUID prescriptionId;
  private UUID testId;

  @BeforeEach
  void setUp() {
    hospitalId = UUID.randomUUID();
    userId = UUID.randomUUID();
    patientId = UUID.randomUUID();
    prescriptionId = UUID.randomUUID();
    testId = UUID.randomUUID();

    lenient().when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    lenient().when(hospitalContext.requireUserId()).thenReturn(userId);

    lenient()
        .when(labOrderTestRepository.save(any(LabOrderTest.class)))
        .thenAnswer(
            i -> {
              LabOrderTest ot = i.getArgument(0);
              if (ot.getId() == null) {
                ot.setId(UUID.randomUUID());
              }
              return ot;
            });
  }

  @Test
  @DisplayName("Should create lab order from recommendation successfully")
  void createLabOrderFromRecommendation_Success() {
    // Arrange
    Prescription prescription = new Prescription();
    prescription.setId(prescriptionId);
    prescription.setHospitalId(hospitalId);
    prescription.setPatientId(patientId);
    prescription.setDoctorId(UUID.randomUUID());
    prescription.setDoctorName("Dr. Test");

    PrescriptionLabItem labItem = new PrescriptionLabItem();
    labItem.setId(UUID.randomUUID());
    labItem.setTestId(testId);
    labItem.setStatus(PrescriptionLabItemStatus.PENDING);
    prescription.setLabItems(new ArrayList<>(Arrays.asList(labItem)));

    LabTest labTest = new LabTest();
    labTest.setId(testId);
    labTest.setTestName("Complete Blood Count");

    LabPrice labPrice = new LabPrice();
    labPrice.setStandardPrice(new BigDecimal("500.00"));

    LabOrderFromRecommendationRequest request = new LabOrderFromRecommendationRequest();
    request.setPrescriptionId(prescriptionId);
    request.setTestIds(Arrays.asList(testId));
    request.setPaymentMode(PaymentMode.CASH);
    request.setAmountPaid(new BigDecimal("500.00"));

    lenient()
        .when(prescriptionRepository.findByIdAndHospitalId(prescriptionId, hospitalId))
        .thenReturn(Optional.of(prescription));
    lenient().when(labTestRepository.findById(testId)).thenReturn(Optional.of(labTest));
    lenient()
        .when(labPriceRepository.findByHospitalIdAndTestId(eq(hospitalId), eq(testId)))
        .thenReturn(Optional.of(labPrice));

    Invoice invoice = new Invoice();
    invoice.setId(UUID.randomUUID());
    invoice.setTotalAmount(labPrice.getStandardPrice());
    lenient()
        .when(invoiceService.getOrCreateInvoice(eq(hospitalId), eq(patientId), any()))
        .thenReturn(invoice);

    lenient()
        .when(labOrderRepository.save(any(LabOrder.class)))
        .thenAnswer(
            i -> {
              LabOrder order = i.getArgument(0);
              order.setId(UUID.randomUUID());
              return order;
            });

    // Act
    LabOrder result = labRecommendationService.createOrderFromRecommendation(request);

    // Assert
    assertNotNull(result);
    assertEquals(patientId, result.getPatientId());
    assertEquals("ORDERED", result.getStatus());

    verify(prescriptionRepository, times(1)).save(prescription);
    verify(labOrderRepository, times(1)).save(any(LabOrder.class));
    // Verify twice: once for order creation, once for payment processing
    verify(invoiceService, times(2)).getOrCreateInvoice(eq(hospitalId), eq(patientId), any());
    verify(paymentService, times(1))
        .recordPayment(eq(hospitalId), any(RecordPaymentRequest.class), eq(userId));
    assertEquals(PrescriptionLabItemStatus.ORDERED, labItem.getStatus());
  }

  @Test
  @DisplayName("Should create lab order for multiple recommendations")
  void createLabOrderFromMultipleRecommendations_Success() {
    // Arrange
    Prescription prescription = new Prescription();
    prescription.setId(prescriptionId);
    prescription.setHospitalId(hospitalId);
    prescription.setPatientId(patientId);
    prescription.setDoctorId(UUID.randomUUID());
    prescription.setDoctorName("Dr. Test");

    UUID testId2 = UUID.randomUUID();

    PrescriptionLabItem labItem1 = new PrescriptionLabItem();
    labItem1.setTestId(testId);
    labItem1.setStatus(PrescriptionLabItemStatus.PENDING);

    PrescriptionLabItem labItem2 = new PrescriptionLabItem();
    labItem2.setTestId(testId2);
    labItem2.setStatus(PrescriptionLabItemStatus.PENDING);

    prescription.setLabItems(new ArrayList<>(Arrays.asList(labItem1, labItem2)));

    LabTest labTest1 = new LabTest();
    labTest1.setId(testId);
    labTest1.setTestName("CBC");

    LabTest labTest2 = new LabTest();
    labTest2.setId(testId2);
    labTest2.setTestName("LFT");

    LabPrice labPrice1 = new LabPrice();
    labPrice1.setStandardPrice(new BigDecimal("500.00"));

    LabPrice labPrice2 = new LabPrice();
    labPrice2.setStandardPrice(new BigDecimal("800.00"));

    LabOrderFromRecommendationRequest request = new LabOrderFromRecommendationRequest();
    request.setPrescriptionId(prescriptionId);
    request.setTestIds(Arrays.asList(testId, testId2));
    request.setPaymentMode(PaymentMode.CARD);
    request.setAmountPaid(new BigDecimal("1300.00"));

    lenient()
        .when(prescriptionRepository.findByIdAndHospitalId(prescriptionId, hospitalId))
        .thenReturn(Optional.of(prescription));
    lenient().when(labTestRepository.findById(testId)).thenReturn(Optional.of(labTest1));
    lenient().when(labTestRepository.findById(testId2)).thenReturn(Optional.of(labTest2));
    lenient()
        .when(labPriceRepository.findByHospitalIdAndTestId(hospitalId, testId))
        .thenReturn(Optional.of(labPrice1));
    lenient()
        .when(labPriceRepository.findByHospitalIdAndTestId(hospitalId, testId2))
        .thenReturn(Optional.of(labPrice2));

    Invoice invoice = new Invoice();
    invoice.setId(UUID.randomUUID());
    invoice.setTotalAmount(new BigDecimal("1300.00"));
    lenient()
        .when(invoiceService.getOrCreateInvoice(eq(hospitalId), eq(patientId), any()))
        .thenReturn(invoice);

    lenient()
        .when(labOrderRepository.save(any(LabOrder.class)))
        .thenAnswer(
            i -> {
              LabOrder order = i.getArgument(0);
              order.setId(UUID.randomUUID());
              return order;
            });

    // Act
    LabOrder result = labRecommendationService.createOrderFromRecommendation(request);

    // Assert
    assertNotNull(result);
    verify(paymentService, times(1))
        .recordPayment(eq(hospitalId), any(RecordPaymentRequest.class), eq(userId));
    assertEquals(PrescriptionLabItemStatus.ORDERED, labItem1.getStatus());
    assertEquals(PrescriptionLabItemStatus.ORDERED, labItem2.getStatus());
  }
}
