package com.res.hms.pharmacy.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.hospital.domain.Hospital;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.pharmacy.application.dto.PharmacyReturnRequest;
import com.res.hms.pharmacy.application.dto.PharmacyReturnResponse;
import com.res.hms.pharmacy.application.dto.PharmacyStockDTO;
import com.res.hms.pharmacy.application.dto.StockRequest;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.domain.PharmacyReturn;
import com.res.hms.pharmacy.domain.PharmacySaleItem;
import com.res.hms.pharmacy.domain.PharmacyStock;
import com.res.hms.pharmacy.domain.PharmacyStockTransaction;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyReturnRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacySaleItemRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockTransactionRepository;
import com.res.hms.tax.application.dto.TaxCalculationResult;
import com.res.hms.tax.application.service.TaxEngineService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class PharmacyStockServiceTest {

  private PharmacyStockRepository pharmacyStockRepository;
  private PharmacyStockTransactionRepository transactionRepository;
  private MedicineRepository medicineRepository;
  private HospitalRepository hospitalRepository;
  private TaxEngineService taxEngineService;
  private HospitalContext hospitalContext;
  private PharmacySaleItemRepository pharmacySaleItemRepository;
  private PharmacyReturnRepository pharmacyReturnRepository;
  private PharmacyStockService pharmacyStockService;

  private final UUID hospitalId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    pharmacyStockRepository = mock(PharmacyStockRepository.class);
    transactionRepository = mock(PharmacyStockTransactionRepository.class);
    medicineRepository = mock(MedicineRepository.class);
    hospitalRepository = mock(HospitalRepository.class);
    taxEngineService = mock(TaxEngineService.class);
    hospitalContext = mock(HospitalContext.class);
    pharmacySaleItemRepository = mock(PharmacySaleItemRepository.class);
    pharmacyReturnRepository = mock(PharmacyReturnRepository.class);

    pharmacyStockService =
        new PharmacyStockService(
            pharmacyStockRepository,
            transactionRepository,
            medicineRepository,
            hospitalRepository,
            taxEngineService,
            hospitalContext,
            pharmacySaleItemRepository,
            pharmacyReturnRepository);

    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
  }

  @Test
  void testStockInSuccess() {
    StockRequest request = new StockRequest();
    request.setMedicineId(UUID.randomUUID());
    request.setBatchNumber("B123");
    request.setQuantity(100);
    request.setPurchasePrice(BigDecimal.valueOf(10));
    request.setSellingPrice(BigDecimal.valueOf(15));
    request.setExpiryDate(LocalDate.now().plusYears(1));

    Hospital hospital = new Hospital();
    hospital.setId(hospitalId);
    hospital.setState("StateX");

    Medicine medicine = new Medicine();
    medicine.setId(request.getMedicineId());
    medicine.setName("MedA");

    when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
    when(medicineRepository.findById(request.getMedicineId())).thenReturn(Optional.of(medicine));
    when(pharmacyStockRepository.findByHospitalIdAndMedicineIdAndBatchNumber(
            eq(hospitalId), eq(medicine.getId()), eq("B123")))
        .thenReturn(Optional.empty());

    TaxCalculationResult taxResult = mock(TaxCalculationResult.class);
    when(taxResult.taxableAmount()).thenReturn(BigDecimal.valueOf(1000));
    when(taxResult.totalTax()).thenReturn(BigDecimal.valueOf(100));
    when(taxResult.grandTotal()).thenReturn(BigDecimal.valueOf(1100));
    when(taxResult.components()).thenReturn(Collections.emptyList());
    when(taxEngineService.calculate(any())).thenReturn(taxResult);

    when(pharmacyStockRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    PharmacyStock result = pharmacyStockService.stockIn(request);

    assertNotNull(result);
    assertEquals(100, result.getQuantity());
    verify(pharmacyStockRepository).save(any(PharmacyStock.class));
    verify(transactionRepository).save(any(PharmacyStockTransaction.class));
  }

  @Test
  void testStockInExistingStock() {
    StockRequest request = new StockRequest();
    request.setMedicineId(UUID.randomUUID());
    request.setBatchNumber("B123");
    request.setQuantity(50);
    request.setPurchasePrice(BigDecimal.valueOf(10));
    request.setSellingPrice(BigDecimal.valueOf(15));
    request.setExpiryDate(LocalDate.now().plusYears(1));
    request.setHsnCode("NEWHSN");

    Hospital hospital = new Hospital();
    hospital.setId(hospitalId);
    hospital.setState("StateX");

    Medicine medicine = new Medicine();
    medicine.setId(request.getMedicineId());
    medicine.setName("MedA");
    medicine.setHsnCode("OLDHSN");

    PharmacyStock existingStock = new PharmacyStock();
    existingStock.setQuantity(20);

    when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.of(hospital));
    when(medicineRepository.findById(request.getMedicineId())).thenReturn(Optional.of(medicine));
    when(pharmacyStockRepository.findByHospitalIdAndMedicineIdAndBatchNumber(any(), any(), any()))
        .thenReturn(Optional.of(existingStock));

    TaxCalculationResult taxResult = mock(TaxCalculationResult.class);
    when(taxResult.taxableAmount()).thenReturn(BigDecimal.valueOf(500));
    when(taxResult.totalTax()).thenReturn(BigDecimal.valueOf(50));
    when(taxResult.grandTotal()).thenReturn(BigDecimal.valueOf(550));
    when(taxResult.components()).thenReturn(Collections.emptyList());
    when(taxEngineService.calculate(any())).thenReturn(taxResult);

    when(pharmacyStockRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    PharmacyStock result = pharmacyStockService.stockIn(request);

    assertEquals(70, result.getQuantity());
    assertEquals("NEWHSN", medicine.getHsnCode());
  }

  @Test
  void testStockInHospitalNotFound() {
    when(hospitalRepository.findById(hospitalId)).thenReturn(Optional.empty());
    assertThrows(ApiException.class, () -> pharmacyStockService.stockIn(new StockRequest()));
  }

  @Test
  void testReturnStockExactQuantity() {
    PharmacyReturnRequest request = new PharmacyReturnRequest();
    request.setSaleItemId(UUID.randomUUID());
    request.setQuantity(10);

    PharmacySaleItem saleItem = new PharmacySaleItem();
    saleItem.setQuantity(10);
    PharmacyStock stock = new PharmacyStock();
    stock.setQuantity(5);
    saleItem.setStock(stock);

    when(pharmacySaleItemRepository.findByIdAndHospitalId(any(), any()))
        .thenReturn(Optional.of(saleItem));
    when(pharmacyReturnRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

    PharmacyReturnResponse response = pharmacyStockService.returnStock(request);
    assertEquals(10, response.getQuantity());
    assertEquals(15, stock.getQuantity());
  }

  @Test
  void testLowStockAlertsInvalidThreshold() {
    Page<PharmacyStock> page = new PageImpl<>(Collections.emptyList());
    when(pharmacyStockRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(page);

    PageResponse<PharmacyStockDTO> result =
        pharmacyStockService.lowStockAlerts(Pageable.unpaged(), "", Map.of("threshold", "abc"));

    assertNotNull(result);
    assertEquals(0, result.content().size());
  }

  @Test
  void testReturnStockSuccess() {
    PharmacyReturnRequest request = new PharmacyReturnRequest();
    request.setSaleItemId(UUID.randomUUID());
    request.setQuantity(5);
    request.setReason("Expired");

    PharmacySaleItem saleItem = new PharmacySaleItem();
    saleItem.setId(request.getSaleItemId());
    saleItem.setQuantity(10);

    PharmacyStock stock = new PharmacyStock();
    stock.setQuantity(50);
    saleItem.setStock(stock);

    when(pharmacySaleItemRepository.findByIdAndHospitalId(request.getSaleItemId(), hospitalId))
        .thenReturn(Optional.of(saleItem));
    when(pharmacyReturnRepository.save(any(PharmacyReturn.class)))
        .thenAnswer(i -> i.getArguments()[0]);

    PharmacyReturnResponse response = pharmacyStockService.returnStock(request);

    assertNotNull(response);
    assertEquals(5, response.getQuantity());
    assertEquals(55, stock.getQuantity());
    verify(pharmacyReturnRepository).save(any(PharmacyReturn.class));
  }

  @Test
  void testReturnStockExceedsQuantity() {
    PharmacyReturnRequest request = new PharmacyReturnRequest();
    request.setSaleItemId(UUID.randomUUID());
    request.setQuantity(15);

    PharmacySaleItem saleItem = new PharmacySaleItem();
    saleItem.setQuantity(10);

    when(pharmacySaleItemRepository.findByIdAndHospitalId(request.getSaleItemId(), hospitalId))
        .thenReturn(Optional.of(saleItem));

    assertThrows(ApiException.class, () -> pharmacyStockService.returnStock(request));
  }

  @Test
  void testExpiryAlerts() {
    PharmacyStock stock = new PharmacyStock();
    stock.setHospitalId(hospitalId);
    Medicine medicine = new Medicine();
    medicine.setId(UUID.randomUUID());
    medicine.setName("MedA");
    stock.setMedicine(medicine);
    stock.setQuantity(10);

    when(pharmacyStockRepository.findExpiringBatches(eq(hospitalId), any(), any()))
        .thenReturn(List.of(stock));
    when(pharmacyStockRepository.availableQuantity(eq(hospitalId), any(), any())).thenReturn(10L);

    List<PharmacyStockDTO> alerts = pharmacyStockService.expiryAlerts(30);

    assertNotNull(alerts);
    assertEquals(1, alerts.size());
    assertEquals("MedA", alerts.get(0).getMedicineName());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testLowStockAlerts() {
    PharmacyStock stock = new PharmacyStock();
    stock.setHospitalId(hospitalId);
    Medicine medicine = new Medicine();
    medicine.setId(UUID.randomUUID());
    medicine.setName("MedA");
    stock.setMedicine(medicine);
    stock.setQuantity(5);

    Page<PharmacyStock> page = new PageImpl<>(List.of(stock));
    when(pharmacyStockRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(page);
    when(pharmacyStockRepository.availableQuantity(eq(hospitalId), any(), any())).thenReturn(5L);

    PageResponse<PharmacyStockDTO> result =
        pharmacyStockService.lowStockAlerts(Pageable.unpaged(), "", Map.of("threshold", "10"));

    assertNotNull(result);
    assertEquals(1, result.content().size());
    assertEquals("MedA", result.content().get(0).getMedicineName());
  }
}
