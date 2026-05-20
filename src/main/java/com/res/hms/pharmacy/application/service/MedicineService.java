package com.res.hms.pharmacy.application.service;

import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.pharmacy.application.dto.MedicineDTO;
import com.res.hms.pharmacy.application.dto.MedicineImportResponse;
import com.res.hms.pharmacy.application.dto.MedicineSummaryDTO;
import com.res.hms.pharmacy.application.dto.PharmacyReturnRequest;
import com.res.hms.pharmacy.application.dto.PharmacyReturnResponse;
import com.res.hms.pharmacy.application.dto.PharmacySaleRequest;
import com.res.hms.pharmacy.application.dto.PharmacySaleResponse;
import com.res.hms.pharmacy.application.dto.PharmacyStockDTO;
import com.res.hms.pharmacy.application.dto.StockImportResponse;
import com.res.hms.pharmacy.application.dto.StockRequest;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.domain.PharmacyReturn;
import com.res.hms.pharmacy.domain.PharmacySale;
import com.res.hms.pharmacy.domain.PharmacySaleItem;
import com.res.hms.pharmacy.domain.PharmacyStock;
import com.res.hms.pharmacy.infrastructure.repository.HospitalMedicineCatalogRepository;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyReturnRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacySaleItemRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacySaleRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineService {

  private static final int DEFAULT_LOW_STOCK_THRESHOLD = 20;

  private final MedicineRepository medicineRepository;
  private final HospitalMedicineCatalogRepository hospitalMedicineCatalogRepository;
  private final PharmacyStockRepository pharmacyStockRepository;
  private final PharmacySaleRepository pharmacySaleRepository;
  private final PharmacySaleItemRepository pharmacySaleItemRepository;
  private final PharmacyReturnRepository pharmacyReturnRepository;
  private final HospitalContext hospitalContext;
  private final com.res.hms.clinical.application.service.PatientVisitService patientVisitService;
  private final com.res.hms.core.service.DomainEventPublisher eventPublisher;
  private final PharmacyExcelService pharmacyExcelService;

  @Transactional(readOnly = true)
  public PageResponse<MedicineDTO> list(int page, int size, String search) {
    PageRequest pageRequest = PageRequest.of(page, size);
    if (hasText(search)) {
      return PageResponse.from(
          medicineRepository.searchMaster(search.trim(), pageRequest).map(this::toDto));
    }
    return PageResponse.from(
        medicineRepository.findAllByOrderByCreatedAtDesc(pageRequest).map(this::toDto));
  }

  @Transactional
  public MedicineImportResponse importExcel(MultipartFile file) {
    return pharmacyExcelService.importMedicines(file);
  }

  @Transactional
  public MedicineDTO create(MedicineDTO request) {
    if (medicineRepository.existsDuplicate(
        request.getName(),
        request.getGenericName(),
        request.getStrength(),
        request.getForm())) {
      throw new ApiException("DUPLICATE_MEDICINE", "Medicine already exists", HttpStatus.CONFLICT);
    }

    Medicine medicine = new Medicine();
    updateFields(medicine, request);

    if (request.getCurrentStock() != null
        && request.getCurrentStock() > 0
        && hasText(request.getBatchNumber())) {
      Optional.ofNullable(hospitalContext.getHospitalIdOrNull())
          .ifPresent(
              hospitalId -> {
                PharmacyStock stock = new PharmacyStock();
                stock.setHospitalId(hospitalId);
                stock.setMedicine(medicine);
                stock.setBatchNumber(request.getBatchNumber());
                stock.setQuantity(request.getCurrentStock());
                stock.setExpiryDate(request.getExpiryDate());
                stock.setPurchasePrice(
                    request.getPurchasePrice() != null
                        ? request.getPurchasePrice()
                        : BigDecimal.ZERO);
                stock.setSellingPrice(
                    request.getSellingPrice() != null
                        ? request.getSellingPrice()
                        : BigDecimal.ZERO);
                pharmacyStockRepository.save(stock);
              });
    }

    return toDto(medicineRepository.save(medicine));
  }

  @Transactional(readOnly = true)
  public MedicineDTO get(UUID id) {
    return toDto(requireMedicine(id));
  }

  @Transactional
  public MedicineDTO update(UUID id, MedicineDTO request) {
    Medicine medicine = requireMedicine(id);
    updateFields(medicine, request);
    return toDto(medicineRepository.save(medicine));
  }

  @Transactional
  public void delete(UUID id) {
    Medicine medicine = requireMedicine(id);
    medicineRepository.delete(medicine);
  }

  @Transactional
  public StockImportResponse importStockExcel(MultipartFile file) {
    return pharmacyExcelService.importStock(file);
  }

  @Transactional
  public PharmacyStockDTO addStock(StockRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Medicine medicine = requireMedicine(request.getMedicineId());

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
    PharmacyStock saved = pharmacyStockRepository.save(stock);
    return toStockDto(saved);
  }

  @Transactional(readOnly = true)
  public PageResponse<MedicineSummaryDTO> listSummary(int page, int size, String search) {
    PageRequest pageRequest = PageRequest.of(page, size);
    org.springframework.data.domain.Page<Medicine> medicinesPage;
    if (hasText(search)) {
      medicinesPage = medicineRepository.searchMaster(search.trim(), pageRequest);
    } else {
      medicinesPage = medicineRepository.findAllByOrderByCreatedAtDesc(pageRequest);
    }

    UUID hospitalId = hospitalContext.getHospitalIdOrNull();
    java.util.Map<UUID, Integer> stockMap = new java.util.HashMap<>();
    if (hospitalId != null && !medicinesPage.isEmpty()) {
      List<UUID> medicineIds = medicinesPage.getContent().stream().map(Medicine::getId).toList();
      List<Object[]> quantities =
          pharmacyStockRepository.availableQuantities(hospitalId, medicineIds, LocalDate.now());
      for (Object[] row : quantities) {
        stockMap.put((UUID) row[0], ((Long) row[1]).intValue());
      }
    }

    return PageResponse.from(
        medicinesPage.map(m -> toSummaryDtoWithStock(m, stockMap.getOrDefault(m.getId(), 0))));
  }

  private MedicineSummaryDTO toSummaryDtoWithStock(Medicine medicine, int stock) {
    return MedicineSummaryDTO.builder()
        .id(medicine.getId())
        .name(medicine.getName())
        .genericName(medicine.getGenericName())
        .composition(medicine.getComposition())
        .strength(medicine.getStrength())
        .form(medicine.getForm())
        .category(medicine.getCategory())
        .manufacturer(medicine.getManufacturer())
        .currentStock(stock)
        .build();
  }

  @Transactional(readOnly = true)
  public PageResponse<PharmacyStockDTO> listStock(
      int page, int size, String search, boolean lowStock, UUID medicineId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PageRequest pageRequest = PageRequest.of(page, size);

    if (medicineId != null) {
      return PageResponse.from(
          pharmacyStockRepository
              .findAllByHospitalIdAndMedicineId(hospitalId, medicineId, pageRequest)
              .map(this::toStockDtoSimple));
    }

    if (lowStock) {
      return PageResponse.from(
          pharmacyStockRepository
              .findAllByHospitalIdAndQuantityLessThan(
                  hospitalId, DEFAULT_LOW_STOCK_THRESHOLD, pageRequest)
              .map(this::toStockDtoSimple));
    }
    if (hasText(search)) {
      return PageResponse.from(
          pharmacyStockRepository
              .searchDetailedStock(hospitalId, search.trim(), pageRequest)
              .map(this::toStockDtoSimple));
    }
    return PageResponse.from(
        pharmacyStockRepository.findDetailedStock(hospitalId, pageRequest).map(this::toStockDtoSimple));
  }

  private PharmacyStockDTO toStockDtoSimple(PharmacyStock stock) {
    return PharmacyStockDTO.builder()
        .id(stock.getId())
        .medicineId(stock.getMedicine().getId())
        .medicineName(stock.getMedicine().getName())
        .genericName(stock.getMedicine().getGenericName())
        .composition(stock.getMedicine().getComposition())
        .batchNumber(stock.getBatchNumber())
        .expiryDate(stock.getExpiryDate())
        .quantity(stock.getQuantity())
        .purchasePrice(stock.getPurchasePrice())
        .sellingPrice(stock.getSellingPrice())
        .price(stock.getSellingPrice())
        .build();
  }

  private final com.res.hms.clinical.application.prescription.repository.PrescriptionRepository prescriptionRepository;
  private final com.res.hms.clinical.infrastructure.repository.AppointmentRepository appointmentRepository;

  @Transactional
  public PharmacySaleResponse createSale(PharmacySaleRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    
    // Idempotency check: Ensure prescription isn't already processed
    if (request.getPrescriptionId() != null) {
      pharmacySaleRepository.findByPrescriptionIdAndHospitalId(request.getPrescriptionId(), hospitalId)
          .ifPresent(s -> {
              throw new ApiException("ALREADY_DISPENSED", 
                  "Prescription already dispensed in Sale #" + s.getSaleNumber(), 
                  HttpStatus.CONFLICT);
          });
    }

    PharmacySale sale = new PharmacySale();
    sale.setHospitalId(hospitalId);
    sale.setPatientId(request.getPatientId());
    sale.setPrescriptionId(request.getPrescriptionId());

    UUID resolvedVisitId = request.getVisitId();
    
    // 1. Try to resolve Visit ID from Prescription
    if (resolvedVisitId == null && request.getPrescriptionId() != null) {
      resolvedVisitId = prescriptionRepository.findById(request.getPrescriptionId())
          .filter(p -> p.getAppointmentId() != null)
          .flatMap(p -> appointmentRepository.findById(p.getAppointmentId()))
          .map(com.res.hms.clinical.domain.Appointment::getVisitId)
          .orElse(null);
    }

    // 2. If still null (Walk-in), create a new quick encounter
    if (resolvedVisitId == null) {
      com.res.hms.clinical.domain.PatientVisit visit = patientVisitService.createVisit(
          hospitalId, request.getPatientId(), com.res.hms.clinical.domain.VisitType.WALKIN);
      resolvedVisitId = visit.getId();
    }
    
    sale.setVisitId(resolvedVisitId);


    BigDecimal total = BigDecimal.ZERO;
    LocalDate today = LocalDate.now();
    for (PharmacySaleRequest.Item requestedItem : request.getItems()) {
      Medicine medicine = requireMedicine(requestedItem.getMedicineId());
      int remaining = requestedItem.getQuantity();
      List<PharmacyStock> batches =
          pharmacyStockRepository.findDispensableBatchesForUpdate(
              hospitalId, medicine.getId(), today);

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
        .collect(Collectors.toList());
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

  public Medicine requireMedicine(UUID id) {
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

  public MedicineSummaryDTO toSummaryDto(Medicine medicine) {
    if (medicine == null) {
      return null;
    }

    MedicineSummaryDTO.MedicineSummaryDTOBuilder builder =
        MedicineSummaryDTO.builder()
            .id(medicine.getId())
            .name(medicine.getName())
            .genericName(medicine.getGenericName())
            .strength(medicine.getStrength())
            .form(medicine.getForm())
            .category(medicine.getCategory())
            .manufacturer(medicine.getManufacturer());

    Optional.ofNullable(hospitalContext.getHospitalIdOrNull())
        .ifPresent(
            hospitalId -> {
              Long total =
                  pharmacyStockRepository.availableQuantity(
                      hospitalId, medicine.getId(), LocalDate.now());
              builder.currentStock(total != null ? total.intValue() : 0);
            });

    return builder.build();
  }

  public MedicineDTO toDto(Medicine medicine) {
    MedicineDTO.MedicineDTOBuilder builder =
        MedicineDTO.builder()
            .id(medicine.getId())
            .name(medicine.getName())
            .genericName(medicine.getGenericName())
            .composition(medicine.getComposition())
            .strength(medicine.getStrength())
            .form(medicine.getForm())
            .scheduleType(medicine.getScheduleType())
            .category(medicine.getCategory())
            .manufacturer(medicine.getManufacturer())
            .temperature(medicine.getStorageTemperature())
            .humidity(medicine.getStorageHumidity())
            .specialNotes(medicine.getSpecialNotes());

    Optional.ofNullable(hospitalContext.getHospitalIdOrNull())
        .ifPresent(
            hospitalId -> {
              hospitalMedicineCatalogRepository
                  .findByHospitalIdAndMedicineId(hospitalId, medicine.getId())
                  .ifPresent(
                      catalog -> {
                        builder.reorderLevel(catalog.getReorderLevel());
                        builder.sellingPrice(catalog.getBaseSellingPrice());
                        builder.taxPercentage(catalog.getTaxPercentage());
                      });
            });

    return builder.build();
  }

  private void updateFields(Medicine medicine, MedicineDTO dto) {
    medicine.setName(dto.getName());
    medicine.setGenericName(dto.getGenericName());
    medicine.setComposition(dto.getComposition());
    medicine.setStrength(dto.getStrength());
    medicine.setForm(dto.getForm());
    medicine.setScheduleType(dto.getScheduleType());
    medicine.setCategory(dto.getCategory());
    medicine.setManufacturer(dto.getManufacturer());
    medicine.setStorageTemperature(dto.getTemperature());
    medicine.setStorageHumidity(dto.getHumidity());
    medicine.setSpecialNotes(dto.getSpecialNotes());

    // Ensure medicine is saved first to have an ID for the catalog association
    medicineRepository.save(medicine);

    Optional.ofNullable(hospitalContext.getHospitalIdOrNull())
        .ifPresent(
            hospitalId -> {
              var catalog =
                  hospitalMedicineCatalogRepository
                      .findByHospitalIdAndMedicineId(hospitalId, medicine.getId())
                      .orElseGet(
                          () -> {
                            var c = new com.res.hms.pharmacy.domain.HospitalMedicineCatalog();
                            c.setHospitalId(hospitalId);
                            c.setMedicine(medicine);
                            return c;
                          });
              catalog.setReorderLevel(dto.getReorderLevel());
              catalog.setBaseSellingPrice(dto.getSellingPrice());
              catalog.setTaxPercentage(dto.getTaxPercentage());
              hospitalMedicineCatalogRepository.save(catalog);
            });
  }

  private boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
