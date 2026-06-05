package com.res.hms.pharmacy.application.service;

import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.SpecificationBuilder;
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
import com.res.hms.pharmacy.domain.PharmacyStockTransactionTax;
import com.res.hms.pharmacy.domain.PharmacyStockTransactionType;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyReturnRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacySaleItemRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockTransactionRepository;
import com.res.hms.tax.application.dto.TaxCalculationRequest;
import com.res.hms.tax.application.dto.TaxCalculationResult;
import com.res.hms.tax.application.service.TaxEngineService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
  private final PharmacySaleItemRepository pharmacySaleItemRepository;
  private final PharmacyReturnRepository pharmacyReturnRepository;

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
            .isTaxInclusive(request.isTaxInclusive())
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

  @Transactional
  public PharmacyReturnResponse returnStock(PharmacyReturnRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PharmacySaleItem saleItem =
        pharmacySaleItemRepository
            .findByIdAndHospitalId(request.getSaleItemId(), hospitalId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Sale item not found", HttpStatus.NOT_FOUND));

    if (request.getQuantity() > saleItem.getQuantity()) {
      throw new ApiException(
          "INVALID_RETURN", "Return quantity exceeds sale quantity", HttpStatus.BAD_REQUEST);
    }

    PharmacyStock stock = saleItem.getStock();
    stock.setQuantity(stock.getQuantity() + request.getQuantity());

    PharmacyReturn pharmacyReturn = new PharmacyReturn();
    pharmacyReturn.setHospitalId(hospitalId);
    pharmacyReturn.setSaleItem(saleItem);
    pharmacyReturn.setStock(stock);
    pharmacyReturn.setQuantity(request.getQuantity());
    pharmacyReturn.setReason(request.getReason());
    pharmacyReturn = pharmacyReturnRepository.save(pharmacyReturn);

    return PharmacyReturnResponse.builder()
        .id(pharmacyReturn.getId())
        .saleItemId(saleItem.getId())
        .stockId(stock.getId())
        .quantity(pharmacyReturn.getQuantity())
        .reason(pharmacyReturn.getReason())
        .build();
  }

  @Transactional(readOnly = true)
  public List<PharmacyStockDTO> expiryAlerts(int days) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    LocalDate today = LocalDate.now();
    return pharmacyStockRepository
        .findExpiringBatches(hospitalId, today, today.plusDays(days))
        .stream()
        .map(this::toStockDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public PageResponse<PharmacyStockDTO> lowStockAlerts(
      Pageable pageable, String search, Map<String, String> filters) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    int threshold = 20;
    if (filters.containsKey("threshold")) {
      try {
        threshold = Integer.parseInt(filters.get("threshold"));
      } catch (Exception ignored) {
      }
    }

    SpecificationBuilder<PharmacyStock> builder =
        new SpecificationBuilder<>(filters, search, List.of("medicine.name", "batchNumber"));

    final int finalThreshold = threshold;
    Specification<PharmacyStock> spec = builder.build();
    Specification<PharmacyStock> fixedSpec =
        (root, query, cb) -> {
          return cb.and(
              cb.equal(root.get("hospitalId"), hospitalId),
              cb.lessThan(root.get("quantity"), finalThreshold));
        };

    return PageResponse.from(
        pharmacyStockRepository
            .findAll(Specification.where(spec).and(fixedSpec), pageable)
            .map(this::toStockDto));
  }

  public PharmacyStockDTO toStockDto(PharmacyStock stock) {
    Long total =
        pharmacyStockRepository.availableQuantity(
            stock.getHospitalId(), stock.getMedicine().getId(), LocalDate.now());
    return PharmacyStockDTO.builder()
        .id(stock.getId())
        .medicineId(stock.getMedicine().getId())
        .medicineName(stock.getMedicine().getName())
        .genericName(stock.getMedicine().getGenericName())
        .composition(stock.getMedicine().getComposition())
        .batchNumber(stock.getBatchNumber())
        .expiryDate(stock.getExpiryDate())
        .quantity(stock.getQuantity())
        .totalQuantity(total.intValue())
        .purchasePrice(stock.getPurchasePrice())
        .sellingPrice(stock.getSellingPrice())
        .price(stock.getSellingPrice())
        .build();
  }

  private boolean hasText(String s) {
    return s != null && !s.isBlank();
  }
}
