package com.res.hms.pharmacy.application.service;

import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.pharmacy.application.dto.PharmacyReturnRequest;
import com.res.hms.pharmacy.application.dto.PharmacyReturnResponse;
import com.res.hms.pharmacy.application.dto.PharmacySaleRequest;
import com.res.hms.pharmacy.application.dto.PharmacySaleResponse;
import com.res.hms.pharmacy.application.dto.PharmacyStockDTO;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.domain.PharmacyReturn;
import com.res.hms.pharmacy.domain.PharmacySale;
import com.res.hms.pharmacy.domain.PharmacySaleItem;
import com.res.hms.pharmacy.domain.PharmacyStock;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyReturnRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacySaleItemRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacySaleRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MedicineSalesService {

  private final MedicineRepository medicineRepository;
  private final PharmacyStockRepository pharmacyStockRepository;
  private final PharmacySaleRepository pharmacySaleRepository;
  private final PharmacySaleItemRepository pharmacySaleItemRepository;
  private final PharmacyReturnRepository pharmacyReturnRepository;
  private final HospitalContext hospitalContext;
  private final com.res.hms.clinical.application.service.PatientVisitService patientVisitService;
  private final com.res.hms.core.service.DomainEventPublisher eventPublisher;
  private final com.res.hms.clinical.application.prescription.repository.PrescriptionRepository
      prescriptionRepository;
  private final com.res.hms.clinical.infrastructure.repository.AppointmentRepository
      appointmentRepository;

  @Transactional
  public PharmacySaleResponse createSale(PharmacySaleRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    validatePrescriptionNotDispensed(request, hospitalId);

    PharmacySale sale = initializeSale(request, hospitalId);
    sale.setVisitId(resolveVisitId(request, hospitalId));
    sale.setTotalAmount(dispenseItems(request.getItems(), hospitalId, sale));
    PharmacySale saved = pharmacySaleRepository.save(sale);

    eventPublisher.publish(
        "pharmacy_sale",
        saved.getId(),
        new com.res.hms.pharmacy.application.event.PharmacySaleCompletedEvent(
            saved.getId(), saved.getPatientId(), hospitalId));

    return toSaleResponse(saved);
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
  public PageResponse<PharmacyStockDTO> lowStockAlerts(int page, int size, int threshold) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return PageResponse.from(
        pharmacyStockRepository
            .findAllByHospitalIdAndQuantityLessThan(
                hospitalId, threshold, PageRequest.of(page, size))
            .map(this::toStockDto));
  }

  private void validatePrescriptionNotDispensed(PharmacySaleRequest request, UUID hospitalId) {
    if (request.getPrescriptionId() == null) {
      return;
    }
    pharmacySaleRepository
        .findByPrescriptionIdAndHospitalId(request.getPrescriptionId(), hospitalId)
        .ifPresent(
            sale -> {
              throw new ApiException(
                  "ALREADY_DISPENSED",
                  "Prescription already dispensed in Sale #" + sale.getSaleNumber(),
                  HttpStatus.CONFLICT);
            });
  }

  private PharmacySale initializeSale(PharmacySaleRequest request, UUID hospitalId) {
    PharmacySale sale = new PharmacySale();
    sale.setHospitalId(hospitalId);
    sale.setPatientId(request.getPatientId());
    sale.setPrescriptionId(request.getPrescriptionId());
    return sale;
  }

  private UUID resolveVisitId(PharmacySaleRequest request, UUID hospitalId) {
    UUID visitId = request.getVisitId();
    if (visitId == null && request.getPrescriptionId() != null) {
      visitId =
          prescriptionRepository
              .findById(request.getPrescriptionId())
              .filter(p -> p.getAppointmentId() != null)
              .flatMap(p -> appointmentRepository.findById(p.getAppointmentId()))
              .map(com.res.hms.clinical.domain.Appointment::getVisitId)
              .orElse(null);
    }
    if (visitId == null) {
      return patientVisitService
          .createVisit(
              hospitalId, request.getPatientId(), com.res.hms.clinical.domain.VisitType.WALKIN)
          .getId();
    }
    return visitId;
  }

  private BigDecimal dispenseItems(
      List<PharmacySaleRequest.Item> items, UUID hospitalId, PharmacySale sale) {
    BigDecimal total = BigDecimal.ZERO;
    LocalDate today = LocalDate.now();
    for (PharmacySaleRequest.Item requestedItem : items) {
      Medicine medicine = requireMedicine(requestedItem.getMedicineId());
      int remaining = requestedItem.getQuantity();
      List<PharmacyStock> batches =
          pharmacyStockRepository.findDispensableBatchesForUpdate(
              hospitalId, medicine.getId(), today);
      for (PharmacyStock batch : batches) {
        remaining = consumeBatch(sale, medicine, requestedItem, remaining, batch);
        total = total.add(sale.getItems().getLast().getSubtotal());
        if (remaining == 0) {
          break;
        }
      }
      if (remaining > 0) {
        throw new ApiException(
            "INSUFFICIENT_STOCK",
            "Insufficient stock for " + medicine.getName(),
            HttpStatus.CONFLICT);
      }
    }
    return total;
  }

  private int consumeBatch(
      PharmacySale sale,
      Medicine medicine,
      PharmacySaleRequest.Item requestedItem,
      int remaining,
      PharmacyStock batch) {
    int consumed = Math.min(remaining, batch.getQuantity());
    batch.setQuantity(batch.getQuantity() - consumed);

    PharmacySaleItem saleItem = new PharmacySaleItem();
    saleItem.setSale(sale);
    saleItem.setMedicine(medicine);
    saleItem.setStock(batch);
    saleItem.setQuantity(consumed);
    saleItem.setUnitPrice(batch.getSellingPrice());
    saleItem.setSubtotal(batch.getSellingPrice().multiply(BigDecimal.valueOf(consumed)));
    sale.getItems().add(saleItem);
    return remaining - consumed;
  }

  private Medicine requireMedicine(UUID id) {
    return medicineRepository
        .findById(id)
        .orElseThrow(
            () -> new ApiException("NOT_FOUND", "Medicine not found", HttpStatus.NOT_FOUND));
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

  private PharmacyStockDTO toStockDto(PharmacyStock stock) {
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
}
