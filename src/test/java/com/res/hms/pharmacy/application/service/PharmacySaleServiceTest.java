package com.res.hms.pharmacy.application.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.res.hms.clinical.application.prescription.repository.PrescriptionRepository;
import com.res.hms.clinical.application.service.PatientVisitService;
import com.res.hms.clinical.infrastructure.repository.PatientRepository;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.finance.application.dto.PaymentDTO;
import com.res.hms.finance.application.service.InvoiceService;
import com.res.hms.finance.application.service.PaymentService;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.PaymentMode;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleRequest;
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleResponse;
import com.res.hms.pharmacy.application.mapper.PharmacySaleMapper;
import com.res.hms.pharmacy.domain.PharmacySale;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacySaleItemRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacySaleRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import com.res.hms.tax.application.dto.TaxCalculationResult;
import com.res.hms.tax.application.service.TaxEngineService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PharmacySaleServiceTest {

  @Mock private PharmacySaleRepository pharmacySaleRepository;
  @Mock private PharmacyStockRepository pharmacyStockRepository;
  @Mock private MedicineRepository medicineRepository;
  @Mock private PharmacySaleItemRepository pharmacySaleItemRepository;
  @Mock private PrescriptionRepository prescriptionRepository;
  @Mock private InvoiceService invoiceService;
  @Mock private HospitalContext hospitalContext;
  @Mock private PatientVisitService patientVisitService;
  @Mock private TaxEngineService taxEngineService;
  @Mock private PatientRepository patientRepository;
  @Mock private PharmacyStockService pharmacyStockService;
  @Mock private HospitalRepository hospitalRepository;
  @Mock private UserAccountRepository userAccountRepository;
  @Mock private PaymentService paymentService;
  @Mock private PharmacySaleMapper pharmacySaleMapper;

  @InjectMocks private PharmacySaleService pharmacySaleService;

  private final UUID hospitalId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    // Basic setup if needed
  }

  private void setupMocksForSale() {
    lenient().when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    lenient().when(hospitalContext.currentUserIdOrNull()).thenReturn(userId);

    com.res.hms.hospital.domain.Hospital mockHospital = new com.res.hms.hospital.domain.Hospital();
    mockHospital.setState("TEST_STATE");
    lenient().when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(mockHospital));

    lenient()
        .when(pharmacySaleRepository.save(any(PharmacySale.class)))
        .thenAnswer(
            i -> {
              PharmacySale s = i.getArgument(0);
              if (s.getId() == null) {
                s.setId(UUID.randomUUID());
              }
              s.setCreatedAt(Instant.now());
              if (s.getTotalAmount() == null) {
                s.setTotalAmount(BigDecimal.ZERO);
              }
              if (s.getStatus() == null) {
                s.setStatus(com.res.hms.pharmacy.domain.PharmacySaleStatus.COMPLETED);
              }
              return s;
            });

    Invoice invoice = new Invoice();
    invoice.setId(UUID.randomUUID());
    invoice.setInvoiceNumber("INV-001");
    lenient()
        .when(invoiceService.getOrCreateInvoice(any(), any(), any(), any(), any()))
        .thenReturn(invoice);

    PaymentDTO mockPayment =
        PaymentDTO.builder().id(UUID.randomUUID()).receiptNumber("REC-001").build();
    lenient().when(paymentService.recordPayment(any(), any(), any())).thenReturn(mockPayment);

    TaxCalculationResult mockTax = mock(TaxCalculationResult.class);
    lenient().when(mockTax.totalTax()).thenReturn(BigDecimal.ZERO);
    lenient().when(taxEngineService.calculate(any())).thenReturn(mockTax);

    lenient()
        .when(pharmacySaleMapper.toResponse(any(PharmacySale.class)))
        .thenAnswer(
            i -> {
              PharmacySale s = i.getArgument(0);
              return QuickPharmacySaleResponse.builder()
                  .saleId(s.getId())
                  .saleNumber(s.getSaleNumber())
                  .invoiceNumber(s.getSaleNumber())
                  .totalAmount(s.getTotalAmount())
                  .build();
            });
  }

  @Test
  void testCreateSaleSuccess() {
    setupMocksForSale();

    QuickPharmacySaleRequest request = new QuickPharmacySaleRequest();
    request.setCustomerName("John Doe");
    request.setCustomerPhone("1234567890");
    request.setItems(Collections.emptyList());
    QuickPharmacySaleRequest.PaymentInfo payment = new QuickPharmacySaleRequest.PaymentInfo();
    payment.setMode(PaymentMode.CASH);
    payment.setAmount(BigDecimal.valueOf(100));
    request.setPayment(payment);

    QuickPharmacySaleResponse response = pharmacySaleService.createSale(request);

    assertNotNull(response);
    assertNotNull(response.getSaleId());
    assertNotNull(response.getInvoiceNumber());
  }

  @Test
  void testGetSale() {
    setupMocksForSale();

    UUID saleId = UUID.randomUUID();
    PharmacySale sale = new PharmacySale();
    sale.setId(saleId);
    sale.setHospitalId(hospitalId);
    sale.setItems(Collections.emptyList());
    sale.setCreatedAt(Instant.now());
    sale.setTotalAmount(BigDecimal.ZERO);
    sale.setStatus(com.res.hms.pharmacy.domain.PharmacySaleStatus.COMPLETED);

    when(pharmacySaleRepository.findByIdAndHospitalId(saleId, hospitalId))
        .thenReturn(Optional.of(sale));

    QuickPharmacySaleResponse result = pharmacySaleService.getSale(saleId);
    assertNotNull(result);
  }

  @Test
  void testCreateSaleWithItems() {
    setupMocksForSale();

    QuickPharmacySaleRequest request = new QuickPharmacySaleRequest();
    request.setItems(
        List.of(
            QuickPharmacySaleRequest.Item.builder()
                .medicineId(UUID.randomUUID())
                .quantity(5)
                .build()));
    QuickPharmacySaleRequest.PaymentInfo payment = new QuickPharmacySaleRequest.PaymentInfo();
    payment.setMode(PaymentMode.UPI);
    payment.setAmount(BigDecimal.valueOf(200));
    request.setPayment(payment);

    com.res.hms.pharmacy.domain.PharmacyStock stock =
        new com.res.hms.pharmacy.domain.PharmacyStock();
    stock.setId(UUID.randomUUID());
    stock.setQuantity(10);
    stock.setSellingPrice(BigDecimal.valueOf(40));
    stock.setMedicine(new com.res.hms.pharmacy.domain.Medicine());
    stock.getMedicine().setId(UUID.randomUUID());
    stock.getMedicine().setName("MedB");

    com.res.hms.clinical.domain.Patient patient = new com.res.hms.clinical.domain.Patient();
    patient.setFullName("Test Patient");

    lenient()
        .when(pharmacyStockRepository.findDispensableBatchesForUpdate(any(), any(), any()))
        .thenReturn(List.of(stock));
    lenient().when(pharmacySaleItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    lenient()
        .when(patientRepository.findByIdAndHospitalId(any(), any()))
        .thenReturn(Optional.of(patient));

    QuickPharmacySaleResponse response = pharmacySaleService.createSale(request);

    assertNotNull(response);
  }

  @Test
  void testCreateSaleWithPatientId() {
    setupMocksForSale();

    UUID patientId = UUID.randomUUID();
    QuickPharmacySaleRequest request = new QuickPharmacySaleRequest();
    request.setPatientId(patientId);
    request.setItems(Collections.emptyList());
    QuickPharmacySaleRequest.PaymentInfo payment = new QuickPharmacySaleRequest.PaymentInfo();
    payment.setMode(PaymentMode.CASH);
    payment.setAmount(BigDecimal.valueOf(100));
    request.setPayment(payment);

    com.res.hms.clinical.domain.Patient patient = new com.res.hms.clinical.domain.Patient();
    patient.setFullName("Jane Smith");
    lenient()
        .when(patientRepository.findByIdAndHospitalId(patientId, hospitalId))
        .thenReturn(Optional.of(patient));

    QuickPharmacySaleResponse response = pharmacySaleService.createSale(request);

    assertNotNull(response);
  }

  @Test
  void testCreateSaleWithPrescription() {
    setupMocksForSale();

    UUID prescriptionId = UUID.randomUUID();
    QuickPharmacySaleRequest request = new QuickPharmacySaleRequest();
    request.setPrescriptionId(prescriptionId);
    request.setItems(Collections.emptyList());
    QuickPharmacySaleRequest.PaymentInfo payment = new QuickPharmacySaleRequest.PaymentInfo();
    payment.setMode(PaymentMode.CASH);
    payment.setAmount(BigDecimal.valueOf(100));
    request.setPayment(payment);

    com.res.hms.clinical.domain.Prescription prescription =
        new com.res.hms.clinical.domain.Prescription();
    prescription.setId(prescriptionId);
    prescription.setHospitalId(hospitalId);
    com.res.hms.clinical.domain.Patient patient = new com.res.hms.clinical.domain.Patient();
    patient.setFullName("Prescribed Patient");
    prescription.setPatient(patient);

    lenient()
        .when(prescriptionRepository.findByIdAndHospitalId(prescriptionId, hospitalId))
        .thenReturn(Optional.of(prescription));

    QuickPharmacySaleResponse response = pharmacySaleService.createSale(request);
    assertNotNull(response);
  }

  @Test
  void testGetSaleByPrescription() {
    setupMocksForSale();

    UUID prescriptionId = UUID.randomUUID();
    PharmacySale sale = new PharmacySale();
    sale.setId(UUID.randomUUID());
    sale.setHospitalId(hospitalId);
    sale.setItems(Collections.emptyList());
    sale.setCreatedAt(Instant.now());
    sale.setTotalAmount(BigDecimal.ZERO);
    sale.setStatus(com.res.hms.pharmacy.domain.PharmacySaleStatus.COMPLETED);

    when(pharmacySaleRepository.findByPrescriptionIdAndHospitalId(prescriptionId, hospitalId))
        .thenReturn(Optional.of(sale));

    QuickPharmacySaleResponse result = pharmacySaleService.getSaleByPrescription(prescriptionId);
    assertNotNull(result);
  }
}
