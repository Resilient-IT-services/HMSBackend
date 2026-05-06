package com.res.hms.pharmacy.service;

import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.Medicine;
import com.res.hms.domain.PharmacyReturn;
import com.res.hms.domain.PharmacySale;
import com.res.hms.domain.PharmacySaleItem;
import com.res.hms.domain.PharmacyStock;
import com.res.hms.pharmacy.dto.PharmacyReturnRequest;
import com.res.hms.pharmacy.dto.PharmacyReturnResponse;
import com.res.hms.pharmacy.dto.PharmacySaleRequest;
import com.res.hms.pharmacy.dto.PharmacySaleResponse;
import com.res.hms.pharmacy.repository.PharmacyReturnRepository;
import com.res.hms.pharmacy.repository.PharmacySaleItemRepository;
import com.res.hms.pharmacy.repository.PharmacySaleRepository;
import com.res.hms.pharmacy.repository.PharmacyStockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PharmacySaleService {

  private final PharmacySaleRepository pharmacySaleRepository;
  private final PharmacySaleItemRepository pharmacySaleItemRepository;
  private final PharmacyStockRepository pharmacyStockRepository;
  private final PharmacyReturnRepository pharmacyReturnRepository;
  private final MedicineMasterService medicineMasterService;
  private final HospitalContext hospitalContext;

  @Transactional
  public PharmacySaleResponse createSale(PharmacySaleRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PharmacySale sale = new PharmacySale();
    sale.setHospitalId(hospitalId);
    sale.setPatientId(request.getPatientId());
    sale.setPrescriptionId(request.getPrescriptionId());

    BigDecimal total = BigDecimal.ZERO;
    LocalDate today = LocalDate.now();
    for (PharmacySaleRequest.Item requestedItem : request.getItems()) {
      Medicine medicine = medicineMasterService.requireMedicine(requestedItem.getMedicineId());
      int remaining = requestedItem.getQuantity();
      List<PharmacyStock> batches =
          pharmacyStockRepository.findDispensableBatchesForUpdate(hospitalId, medicine.getId(), today);

      for (PharmacyStock batch : batches) {
        if (remaining == 0) {
          break;
        }
        int consumed = Math.min(remaining, batch.getQuantity());
        batch.setQuantity(batch.getQuantity() - consumed);
        remaining -= consumed;

        PharmacySaleItem saleItem = new PharmacySaleItem();
        saleItem.setSale(sale);
        saleItem.setMedicine(medicine);
        saleItem.setStock(batch);
        saleItem.setQuantity(consumed);
        saleItem.setUnitPrice(batch.getSellingPrice());
        saleItem.setSubtotal(batch.getSellingPrice().multiply(BigDecimal.valueOf(consumed)));
        sale.getItems().add(saleItem);
        total = total.add(saleItem.getSubtotal());
      }

      if (remaining > 0) {
        throw new ApiException(
            "INSUFFICIENT_STOCK",
            "Insufficient stock for " + medicine.getName(),
            HttpStatus.CONFLICT);
      }
    }

    sale.setTotalAmount(total);
    return toSaleResponse(pharmacySaleRepository.save(sale));
  }

  @Transactional
  public PharmacyReturnResponse returnStock(PharmacyReturnRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PharmacySaleItem saleItem =
        pharmacySaleItemRepository
            .findByIdAndHospitalId(request.getSaleItemId(), hospitalId)
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Sale item not found", HttpStatus.NOT_FOUND));

    if (request.getQuantity() > saleItem.getQuantity()) {
      throw new ApiException("INVALID_RETURN", "Return quantity exceeds sale quantity", HttpStatus.BAD_REQUEST);
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

  private PharmacySaleResponse toSaleResponse(PharmacySale sale) {
    List<PharmacySaleResponse.Item> items = new ArrayList<>();
    for (PharmacySaleItem item : sale.getItems()) {
      items.add(
          PharmacySaleResponse.Item.builder()
              .id(item.getId())
              .medicineId(item.getMedicine().getId())
              .medicineName(item.getMedicine().getName())
              .stockId(item.getStock().getId())
              .batchNumber(item.getStock().getBatchNumber())
              .expiryDate(item.getStock().getExpiryDate())
              .quantity(item.getQuantity())
              .unitPrice(item.getUnitPrice())
              .subtotal(item.getSubtotal())
              .build());
    }
    return PharmacySaleResponse.builder()
        .id(sale.getId())
        .patientId(sale.getPatientId())
        .prescriptionId(sale.getPrescriptionId())
        .totalAmount(sale.getTotalAmount())
        .items(items)
        .build();
  }
}
