package com.res.hms.pharmacy.application.service;

import com.res.hms.core.api.PageResponse;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.pharmacy.application.dto.MedicineDTO;
import com.res.hms.pharmacy.application.dto.MedicineImportResponse;
import com.res.hms.pharmacy.application.dto.MedicineSummaryDTO;
import com.res.hms.pharmacy.application.dto.PharmacyStockDTO;
import com.res.hms.pharmacy.application.dto.StockImportResponse;
import com.res.hms.pharmacy.application.dto.StockRequest;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.domain.PharmacyStock;
import com.res.hms.pharmacy.infrastructure.repository.HospitalMedicineCatalogRepository;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
  private final HospitalContext hospitalContext;
  private final PharmacyExcelService pharmacyExcelService;
  private final PharmacySaleService pharmacySaleService;

  private Specification<Medicine> getFilterSpec(String search) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (hasText(search)) {
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("genericName")), pattern),
                cb.like(cb.lower(root.get("composition")), pattern)));
      }
      return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  @Transactional(readOnly = true)
  public PageResponse<MedicineDTO> list(int page, int size, String search) {
    Sort sort = hasText(search) ? Sort.by("name").ascending() : Sort.by("createdAt").descending();
    Pageable pageable = PageRequest.of(page, size, sort);
    Specification<Medicine> spec = getFilterSpec(search);
    Page<Medicine> results = medicineRepository.findAll(spec, pageable);
    return PageResponse.from(results.map(this::toDto));
  }

  @Transactional
  public MedicineImportResponse importExcel(MultipartFile file) {
    return pharmacyExcelService.importMedicines(file);
  }

  @Transactional
  public MedicineDTO create(MedicineDTO request) {
    if (medicineRepository.existsDuplicate(
        request.getName(), request.getGenericName(), request.getStrength(), request.getForm())) {
      throw new ApiException("DUPLICATE_MEDICINE", "Medicine already exists", HttpStatus.CONFLICT);
    }
    if (request.getExpiryDate() != null && request.getExpiryDate().isBefore(LocalDate.now())) {
      throw new ApiException(
          "INVALID_EXPIRY", "Initial stock expiry cannot be in the past", HttpStatus.BAD_REQUEST);
    }
    if (request.getSellingPrice() != null
        && request.getPurchasePrice() != null
        && request.getSellingPrice().compareTo(request.getPurchasePrice()) < 0) {
      throw new ApiException(
          "INVALID_PRICE",
          "Selling price cannot be less than purchase price",
          HttpStatus.BAD_REQUEST);
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
    if (request.getExpiryDate() != null && request.getExpiryDate().isBefore(LocalDate.now())) {
      throw new ApiException(
          "INVALID_EXPIRY", "Expiry date cannot be in the past", HttpStatus.BAD_REQUEST);
    }
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
  public void exportStockExcel(jakarta.servlet.http.HttpServletResponse response)
      throws java.io.IOException {
    UUID hospitalId = hospitalContext.requireHospitalId();
    List<PharmacyStock> allStock =
        pharmacyStockRepository.findByHospitalIdOrderByMedicineNameAsc(hospitalId);

    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename=pharmacy_stock_export.xlsx");

    try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        var outputStream = response.getOutputStream()) {
      var sheet = workbook.createSheet("Stock Inventory");

      // Header Style
      var headerStyle = workbook.createCellStyle();
      headerStyle.setFillForegroundColor(
          org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
      headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
      var font = workbook.createFont();
      font.setBold(true);
      headerStyle.setFont(font);

      // Create Header Row
      var headerRow = sheet.createRow(0);
      String[] columns = {
        "Medicine Name",
        "Batch Number",
        "Expiry Date",
        "Quantity",
        "Purchase Price",
        "Selling Price",
        "Low Stock Label"
      };
      for (int i = 0; i < columns.length; i++) {
        var cell = headerRow.createCell(i);
        cell.setCellValue(columns[i]);
        cell.setCellStyle(headerStyle);
      }

      // Populate Data
      int rowIdx = 1;
      for (PharmacyStock stock : allStock) {
        var row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(stock.getMedicine().getName());
        row.createCell(1).setCellValue(stock.getBatchNumber());
        row.createCell(2)
            .setCellValue(stock.getExpiryDate() != null ? stock.getExpiryDate().toString() : "");
        row.createCell(3).setCellValue(stock.getQuantity());
        row.createCell(4).setCellValue(stock.getPurchasePrice().doubleValue());
        row.createCell(5).setCellValue(stock.getSellingPrice().doubleValue());

        // Fetch reorder level from medicine entity
        int reorderLevel = stock.getMedicine().getThresholdQuantity();
        row.createCell(6).setCellValue(reorderLevel);
      }

      for (int i = 0; i < columns.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(outputStream);
    }
  }

  @Transactional(readOnly = true)
  public PageResponse<MedicineSummaryDTO> listSummary(int page, int size, String search) {
    Sort sort = hasText(search) ? Sort.by("name").ascending() : Sort.by("createdAt").descending();
    Pageable pageable = PageRequest.of(page, size, sort);
    Specification<Medicine> spec = getFilterSpec(search);
    org.springframework.data.domain.Page<Medicine> medicinesPage =
        medicineRepository.findAll(spec, pageable);

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
        .manufacturer(medicine.getManufacturer())
        .currentStock(stock)
        .thresholdQuantity(medicine.getThresholdQuantity())
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
              .findLowStock(hospitalId, pageRequest)
              .map(this::toStockDtoSimple));
    }
    if (hasText(search)) {
      return PageResponse.from(
          pharmacyStockRepository
              .searchDetailedStock(hospitalId, search.trim(), pageRequest)
              .map(this::toStockDtoSimple));
    }
    return PageResponse.from(
        pharmacyStockRepository
            .findDetailedStock(hospitalId, pageRequest)
            .map(this::toStockDtoSimple));
  }

  private PharmacyStockDTO toStockDtoSimple(PharmacyStock stock) {
    int threshold = stock.getMedicine().getThresholdQuantity();

    return PharmacyStockDTO.builder()
        .id(stock.getId())
        .medicineId(stock.getMedicine().getId())
        .medicineName(stock.getMedicine().getName())
        .genericName(stock.getMedicine().getGenericName())
        .composition(stock.getMedicine().getComposition())
        .batchNumber(stock.getBatchNumber())
        .expiryDate(stock.getExpiryDate())
        .quantity(stock.getQuantity())
        .reorderLevel(threshold)
        .purchasePrice(stock.getPurchasePrice())
        .sellingPrice(stock.getSellingPrice())
        .price(stock.getSellingPrice())
        .build();
  }

  private PharmacyStockDTO toStockDto(PharmacyStock stock) {
    Long total =
        pharmacyStockRepository.availableQuantity(
            stock.getHospitalId(), stock.getMedicine().getId(), LocalDate.now());

    int threshold = stock.getMedicine().getThresholdQuantity();

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
        .reorderLevel(threshold)
        .purchasePrice(stock.getPurchasePrice())
        .sellingPrice(stock.getSellingPrice())
        .price(stock.getSellingPrice())
        .build();
  }

  public Medicine requireMedicine(UUID id) {
    return medicineRepository
        .findById(id)
        .orElseThrow(
            () -> new ApiException("NOT_FOUND", "Medicine not found", HttpStatus.NOT_FOUND));
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
            .thresholdQuantity(medicine.getThresholdQuantity())
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
            .hsnCode(medicine.getHsnCode())
            .thresholdQuantity(medicine.getThresholdQuantity())
            .manufacturer(medicine.getManufacturer());

    Optional.ofNullable(hospitalContext.getHospitalIdOrNull())
        .ifPresent(
            hospitalId -> {
              hospitalMedicineCatalogRepository
                  .findByHospitalIdAndMedicineId(hospitalId, medicine.getId())
                  .ifPresent(
                      catalog -> {
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
    medicine.setHsnCode(dto.getHsnCode());

    medicine.setManufacturer(dto.getManufacturer());

    medicine.setThresholdQuantity(
        dto.getThresholdQuantity() != null ? dto.getThresholdQuantity() : 10);
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
              catalog.setBaseSellingPrice(dto.getSellingPrice());
              catalog.setTaxPercentage(dto.getTaxPercentage());
              hospitalMedicineCatalogRepository.save(catalog);

              // Also sync tax percentage to medicine master if needed,
              // but current design uses catalog for per-hospital tax.
              // I'll update the master as well for global reference.
              if (dto.getTaxPercentage() != null) {
                medicine.setTaxPercentage(dto.getTaxPercentage());
                medicineRepository.save(medicine);
              }
            });
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
