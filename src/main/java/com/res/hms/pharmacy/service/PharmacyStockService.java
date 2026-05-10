package com.res.hms.pharmacy.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.Medicine;
import com.res.hms.domain.PharmacyStock;
import com.res.hms.pharmacy.dto.PharmacyStockDTO;
import com.res.hms.pharmacy.dto.StockRequest;
import com.res.hms.pharmacy.mapper.PharmacyStockMapper;
import com.res.hms.pharmacy.repository.PharmacySaleItemRepository;
import com.res.hms.pharmacy.repository.PharmacyStockRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PharmacyStockService {

  private static final int DEFAULT_LOW_STOCK_THRESHOLD = 20;

  private final PharmacyStockRepository pharmacyStockRepository;
  private final PharmacySaleItemRepository pharmacySaleItemRepository;
  private final HospitalContext hospitalContext;
  private final MedicineMasterService medicineMasterService;
  private final PharmacyStockMapper pharmacyStockMapper;

  @Transactional
  public PharmacyStockDTO addStock(StockRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Medicine medicine = medicineMasterService.requireMedicine(request.getMedicineId());
    PharmacyStock stock =
        pharmacyStockRepository
            .findByHospitalIdAndMedicineIdAndBatchNumber(
                hospitalId, request.getMedicineId(), request.getBatchNumber())
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
    return toDto(pharmacyStockRepository.save(stock));
  }

  @Transactional(readOnly = true)
  public PageResponse<PharmacyStockDTO> listStock(int page, int size, boolean lowStock) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PageRequest pageRequest = PageRequest.of(page, size);
    if (lowStock) {
      return PageResponse.from(
          pharmacyStockRepository
              .findAllByHospitalIdAndQuantityLessThan(hospitalId, DEFAULT_LOW_STOCK_THRESHOLD, pageRequest)
              .map(this::toDto));
    }
    return PageResponse.from(pharmacyStockRepository.findDetailedStock(hospitalId, pageRequest).map(this::toDto));
  }

  @Transactional(readOnly = true)
  public List<PharmacyStockDTO> expiryAlerts(int days) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    LocalDate today = LocalDate.now();
    return pharmacyStockRepository.findExpiringBatches(hospitalId, today, today.plusDays(days)).stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public PageResponse<PharmacyStockDTO> lowStockAlerts(int page, int size, int threshold) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return PageResponse.from(
        pharmacyStockRepository
            .findAllByHospitalIdAndQuantityLessThan(
                hospitalId, threshold, PageRequest.of(page, size))
            .map(this::toDto));
  }

  public PharmacyStockDTO toDto(PharmacyStock stock) {
    Long total =
        pharmacyStockRepository.availableQuantity(
            stock.getHospitalId(), stock.getMedicine().getId(), LocalDate.now());
    return pharmacyStockMapper.toDto(stock, total);
  }

  @Transactional
  public void deleteStock(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PharmacyStock stock =
        pharmacyStockRepository
            .findByIdAndHospitalId(id, hospitalId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Stock not found", HttpStatus.NOT_FOUND));

    if (pharmacySaleItemRepository.existsByStockId(id)) {
      throw new ApiException(
          "STOCK_IN_USE",
          "Cannot delete stock that has been used in sales. Adjust quantity instead.",
          HttpStatus.CONFLICT);
    }

    pharmacyStockRepository.delete(stock);
  }
}
