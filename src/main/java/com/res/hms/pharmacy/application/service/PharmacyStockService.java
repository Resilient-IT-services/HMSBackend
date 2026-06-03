package com.res.hms.pharmacy.application.service;

import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.hospital.domain.Hospital;
import com.res.hms.hospital.infrastructure.repository.HospitalRepository;
import com.res.hms.pharmacy.application.dto.StockRequest;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.domain.PharmacyStock;
import com.res.hms.pharmacy.domain.PharmacyStockTransaction;
import com.res.hms.pharmacy.domain.PharmacyStockTransactionTax;
import com.res.hms.pharmacy.domain.PharmacyStockTransactionType;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockTransactionRepository;
import com.res.hms.tax.application.dto.TaxCalculationRequest;
import com.res.hms.tax.application.dto.TaxCalculationResult;
import com.res.hms.tax.application.service.TaxEngineService;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PharmacyStockService {

  private final PharmacyStockRepository pharmacyStockRepository;
  private final PharmacyStockTransactionRepository transactionRepository;
  private final MedicineRepository medicineRepository;
  private final HospitalRepository hospitalRepository;
  private final TaxEngineService taxEngineService;
  private final HospitalContext hospitalContext;

  @Transactional
  public PharmacyStock stockIn(StockRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Hospital hospital =
        hospitalRepository
            .findById(hospitalId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Hospital not found", HttpStatus.NOT_FOUND));

    Medicine medicine =
        medicineRepository
            .findById(request.getMedicineId())
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Medicine not found", HttpStatus.NOT_FOUND));

    // 1. Handle HSN Auto-propagation
    String effectiveHsn = request.getHsnCode();
    if (!hasText(effectiveHsn)) {
      effectiveHsn = medicine.getHsnCode();
    } else if (!effectiveHsn.equals(medicine.getHsnCode())) {
      // Learning: Update master if user provided a different HSN
      medicine.setHsnCode(effectiveHsn);
      medicineRepository.save(medicine);
    }

    // 2. Calculate Taxes for the purchase
    BigDecimal purchasePrice = request.getPurchasePrice();
    BigDecimal quantity = BigDecimal.valueOf(request.getQuantity());
    BigDecimal totalAmount = purchasePrice.multiply(quantity);

    TaxCalculationRequest taxRequest =
        TaxCalculationRequest.builder()
            .hospitalId(hospitalId)
            .itemType("MEDICINE")
            .itemCode(medicine.getName())
            .hsnOrSacCode(effectiveHsn)
            .quantity(quantity)
            .unitPrice(purchasePrice)
            .taxableAmount(totalAmount)
            .isTaxInclusive(
                request.isTaxInclusive()) // We need to add this to TaxCalculationRequest
            .hospitalState(hospital.getState())
            .billingState(
                hasText(request.getSupplierState())
                    ? request.getSupplierState()
                    : hospital.getState())
            .build();

    TaxCalculationResult taxResult = taxEngineService.calculate(taxRequest);

    // 3. Update Current Stock
    PharmacyStock stock =
        pharmacyStockRepository
            .findByHospitalIdAndMedicineIdAndBatchNumber(
                hospitalId, medicine.getId(), request.getBatchNumber())
            .orElseGet(
                () -> {
                  PharmacyStock s = new PharmacyStock();
                  s.setHospitalId(hospitalId);
                  s.setMedicine(medicine);
                  s.setBatchNumber(request.getBatchNumber());
                  s.setQuantity(0);
                  return s;
                });

    stock.setExpiryDate(request.getExpiryDate());
    stock.setQuantity(stock.getQuantity() + request.getQuantity());
    stock.setPurchasePrice(request.getPurchasePrice());
    stock.setSellingPrice(request.getSellingPrice());
    PharmacyStock savedStock = pharmacyStockRepository.save(stock);

    // 4. Create Transaction Record (Fiscal Record)
    PharmacyStockTransaction tx = new PharmacyStockTransaction();
    tx.setHospitalId(hospitalId);
    tx.setMedicine(medicine);
    tx.setBatchNumber(request.getBatchNumber());
    tx.setExpiryDate(request.getExpiryDate());
    tx.setQuantity(request.getQuantity());
    tx.setPurchasePrice(request.getPurchasePrice());
    tx.setSellingPrice(request.getSellingPrice());
    tx.setTaxableAmount(taxResult.taxableAmount());
    tx.setTaxAmount(taxResult.totalTax());
    tx.setNetAmount(taxResult.grandTotal());
    tx.setType(PharmacyStockTransactionType.PURCHASE);
    tx.setSupplierState(request.getSupplierState());

    // 5. Create Tax Snapshots
    taxResult
        .components()
        .forEach(
            comp -> {
              PharmacyStockTransactionTax taxSnapshot = new PharmacyStockTransactionTax();
              taxSnapshot.setTransaction(tx);
              taxSnapshot.setTaxCode(comp.taxCode());
              taxSnapshot.setTaxName(comp.taxName());
              taxSnapshot.setTaxRate(comp.taxRate());
              taxSnapshot.setTaxAmount(comp.taxAmount());
              taxSnapshot.setTaxableAmount(taxResult.taxableAmount());
              tx.getTaxes().add(taxSnapshot);
            });

    transactionRepository.save(tx);

    return savedStock;
  }

  private boolean hasText(String s) {
    return s != null && !s.isBlank();
  }
}
