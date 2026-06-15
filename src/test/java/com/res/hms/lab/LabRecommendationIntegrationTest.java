package com.res.hms.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.res.hms.clinical.application.prescription.repository.PrescriptionRepository;
import com.res.hms.clinical.domain.Prescription;
import com.res.hms.clinical.domain.PrescriptionLabItem;
import com.res.hms.clinical.domain.PrescriptionLabItemStatus;
import com.res.hms.core.tenant.HospitalContext;
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

  @Mock private LabOrderRepository orderRepository;
  @Mock private LabOrderTestRepository orderTestRepository;
  @Mock private LabTestRepository testRepository;
  @Mock private LabPriceRepository priceRepository;
  @Mock private PrescriptionRepository prescriptionRepository;
  @Mock private InvoiceService invoiceService;
  @Mock private PaymentService paymentService;
  @Mock private HospitalContext hospitalContext;
  @InjectMocks private LabRecommendationService labRecommendationService;

  private UUID hospitalId;
  private UUID userId;
  private UUID patientId;
  private UUID test1Id;
  private UUID test2Id;

  @BeforeEach
  void setUp() {
    hospitalId = UUID.randomUUID();
    userId = UUID.randomUUID();
    patientId = UUID.randomUUID();
    test1Id = UUID.randomUUID();
    test2Id = UUID.randomUUID();
  }

  @Test
  @DisplayName("Recommendation: Create Order from Prescription with Extra Tests and Payment")
  void testCreateOrderFromRecommendation() {
    // Given
    UUID prescriptionId = UUID.randomUUID();
    Prescription prescription = new Prescription();
    prescription.setId(prescriptionId);
    prescription.setHospitalId(hospitalId);
    prescription.setPatientId(patientId);
    prescription.setLabItems(new ArrayList<>());

    PrescriptionLabItem item1 = new PrescriptionLabItem();
    item1.setTestId(test1Id);
    item1.setTestName("Test 1");
    item1.setStatus(PrescriptionLabItemStatus.PENDING);
    prescription.getLabItems().add(item1);

    lenient().when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    lenient().when(hospitalContext.requireUserId()).thenReturn(userId);
    lenient()
        .when(prescriptionRepository.findByIdAndHospitalId(prescriptionId, hospitalId))
        .thenReturn(Optional.of(prescription));

    // Mock Price for Test 1
    LabPrice price1 = new LabPrice();
    price1.setStandardPrice(new BigDecimal("500.00"));
    lenient()
        .when(priceRepository.findByHospitalIdAndTestId(hospitalId, test1Id))
        .thenReturn(Optional.of(price1));

    // Mock Price for Extra Test 2
    LabPrice price2 = new LabPrice();
    price2.setStandardPrice(new BigDecimal("300.00"));
    lenient()
        .when(priceRepository.findByHospitalIdAndTestId(hospitalId, test2Id))
        .thenReturn(Optional.of(price2));

    // Mock Test Repository for Extra Test
    LabTest test2 = new LabTest();
    test2.setId(test2Id);
    test2.setTestName("Extra Test 2");
    lenient().when(testRepository.findById(test2Id)).thenReturn(Optional.of(test2));

    LabTest test1 = new LabTest();
    test1.setId(test1Id);
    test1.setTestName("Test 1");
    lenient().when(testRepository.findById(test1Id)).thenReturn(Optional.of(test1));

    // Mock Invoice
    Invoice invoice = new Invoice();
    invoice.setId(UUID.randomUUID());
    lenient()
        .when(invoiceService.getOrCreateInvoice(any(), any(), nullable(UUID.class)))
        .thenReturn(invoice);

    // Mock Lab Order and Order Test saves
    lenient().when(orderRepository.save(any(LabOrder.class))).thenAnswer(i -> i.getArgument(0));
    lenient()
        .when(orderTestRepository.save(any(LabOrderTest.class)))
        .thenAnswer(
            i -> {
              LabOrderTest ot = i.getArgument(0);
              ot.setId(UUID.randomUUID());
              return ot;
            });

    // Request including both recommended and extra tests
    LabOrderFromRecommendationRequest request = new LabOrderFromRecommendationRequest();
    request.setPrescriptionId(prescriptionId);
    request.setTestIds(Arrays.asList(test1Id, test2Id));
    request.setPaymentMode(PaymentMode.UPI);
    request.setAmountPaid(new BigDecimal("800.00"));

    // When
    LabOrder result = labRecommendationService.createOrderFromRecommendation(request);

    // Then
    assertNotNull(result);
    assertEquals("ORDERED", result.getStatus());

    // Verify Invoice Items added (one for each test)
    verify(invoiceService, times(2))
        .addItem(
            eq(invoice.getId()),
            anyString(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any());

    // Verify Lab Order Tests created
    verify(orderTestRepository, times(2)).save(any(LabOrderTest.class));

    // Verify Payment Recorded
    verify(paymentService, times(1)).recordPayment(eq(hospitalId), any(), eq(userId));

    // Verify Recommendation Status Updated
    assertEquals(PrescriptionLabItemStatus.ORDERED, item1.getStatus());
    verify(prescriptionRepository, times(1)).save(prescription);
  }
}
