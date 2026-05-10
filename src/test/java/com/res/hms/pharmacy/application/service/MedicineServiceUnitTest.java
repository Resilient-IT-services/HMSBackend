package com.res.hms.pharmacy.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.pharmacy.application.dto.PharmacyReturnRequest;
import com.res.hms.pharmacy.application.dto.PharmacySaleRequest;
import com.res.hms.pharmacy.application.dto.StockRequest;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.domain.PharmacySaleItem;
import com.res.hms.pharmacy.domain.PharmacyStock;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyReturnRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacySaleItemRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacySaleRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import com.res.hms.core.service.DomainEventPublisher;

@ExtendWith(MockitoExtension.class)
class MedicineServiceUnitTest {

    @Mock private MedicineRepository medicineRepository;
    @Mock private PharmacyStockRepository pharmacyStockRepository;
    @Mock private PharmacySaleRepository pharmacySaleRepository;
    @Mock private PharmacySaleItemRepository pharmacySaleItemRepository;
    @Mock private PharmacyReturnRepository pharmacyReturnRepository;
    @Mock private HospitalContext hospitalContext;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private PharmacyExcelService pharmacyExcelService;

    @InjectMocks
    private MedicineService medicineService;

    private UUID hospitalId;
    private UUID medicineId;
    private Medicine medicine;

    @BeforeEach
    void setUp() {
        hospitalId = UUID.randomUUID();
        medicineId = UUID.randomUUID();
        medicine = new Medicine();
        medicine.setId(medicineId);
        medicine.setName("Test Medicine");
    }

    @Test
    void testAddStock_ExistingBatch() {
        when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        
        PharmacyStock existingStock = new PharmacyStock();
        existingStock.setHospitalId(hospitalId);
        existingStock.setMedicine(medicine);
        existingStock.setBatchNumber("B123");
        existingStock.setQuantity(10);
        
        when(pharmacyStockRepository.findByHospitalIdAndMedicineIdAndBatchNumber(hospitalId, medicineId, "B123"))
            .thenReturn(Optional.of(existingStock));
        when(pharmacyStockRepository.availableQuantity(any(), any(), any())).thenReturn(20L);
        when(pharmacyStockRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        StockRequest request = StockRequest.builder()
            .medicineId(medicineId)
            .batchNumber("B123")
            .quantity(5)
            .purchasePrice(BigDecimal.TEN)
            .sellingPrice(BigDecimal.valueOf(15))
            .build();

        var result = medicineService.addStock(request);

        assertEquals(15, result.getQuantity());
        assertEquals(BigDecimal.valueOf(15), result.getSellingPrice());
        verify(pharmacyStockRepository).save(existingStock);
    }

    @Test
    void testCreateSale_InsufficientStock() {
        when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
        when(medicineRepository.findById(medicineId)).thenReturn(Optional.of(medicine));
        
        PharmacyStock stock = new PharmacyStock();
        stock.setQuantity(5);
        stock.setSellingPrice(BigDecimal.TEN);
        
        when(pharmacyStockRepository.findDispensableBatchesForUpdate(eq(hospitalId), eq(medicineId), any()))
            .thenReturn(List.of(stock));

        PharmacySaleRequest request = new PharmacySaleRequest();
        PharmacySaleRequest.Item item = new PharmacySaleRequest.Item();
        item.setMedicineId(medicineId);
        item.setQuantity(10);
        request.setItems(List.of(item));

        ApiException ex = assertThrows(ApiException.class, () -> medicineService.createSale(request));
        assertEquals("INSUFFICIENT_STOCK", ex.getCode());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void testReturnStock_InvalidQuantity() {
        when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
        
        PharmacySaleItem saleItem = new PharmacySaleItem();
        saleItem.setQuantity(5);
        
        when(pharmacySaleItemRepository.findByIdAndHospitalId(any(), eq(hospitalId)))
            .thenReturn(Optional.of(saleItem));

        PharmacyReturnRequest request = new PharmacyReturnRequest();
        request.setSaleItemId(UUID.randomUUID());
        request.setQuantity(10); // More than sold

        ApiException ex = assertThrows(ApiException.class, () -> medicineService.returnStock(request));
        assertEquals("INVALID_RETURN", ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void testRequireMedicine_NotFound() {
        when(medicineRepository.findById(any())).thenReturn(Optional.empty());
        
        assertThrows(ApiException.class, () -> medicineService.requireMedicine(UUID.randomUUID()));
    }
}
