package com.res.hms.pharmacy.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.Medicine;
import com.res.hms.domain.PharmacyReturn;
import com.res.hms.domain.PharmacySale;
import com.res.hms.domain.PharmacySaleItem;
import com.res.hms.domain.PharmacyStock;
import com.res.hms.pharmacy.dto.MedicineDTO;
import com.res.hms.pharmacy.dto.MedicineImportResponse;
import com.res.hms.pharmacy.dto.PharmacyReturnRequest;
import com.res.hms.pharmacy.dto.PharmacyReturnResponse;
import com.res.hms.pharmacy.dto.PharmacySaleRequest;
import com.res.hms.pharmacy.dto.PharmacySaleResponse;
import com.res.hms.pharmacy.dto.PharmacyStockDTO;
import com.res.hms.pharmacy.dto.StockRequest;
import com.res.hms.repository.MedicineRepository;
import com.res.hms.repository.PharmacyReturnRepository;
import com.res.hms.repository.PharmacySaleItemRepository;
import com.res.hms.repository.PharmacySaleRepository;
import com.res.hms.repository.PharmacyStockRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
  private final PharmacyStockRepository pharmacyStockRepository;
  private final PharmacySaleRepository pharmacySaleRepository;
  private final PharmacySaleItemRepository pharmacySaleItemRepository;
  private final PharmacyReturnRepository pharmacyReturnRepository;
  private final HospitalContext hospitalContext;

  @Transactional(readOnly = true)
  public PageResponse<MedicineDTO> list(int page, int size, String search) {
    PageRequest pageRequest = PageRequest.of(page, size);
    if (hasText(search)) {
      return PageResponse.from(medicineRepository.searchMaster(search.trim(), pageRequest).map(this::toDto));
    }
    return PageResponse.from(medicineRepository.findAllByOrderByCreatedAtDesc(pageRequest).map(this::toDto));
  }

  @Transactional(readOnly = true)
  public PageResponse<MedicineDTO> searchAvailable(int page, int size, String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return PageResponse.from(
        medicineRepository
            .searchAvailableForHospital(hospitalId, search == null ? "" : search.trim(), PageRequest.of(page, size))
            .map(this::toDto));
  }

  @Transactional(readOnly = true)
  public MedicineDTO get(UUID id) {
    return toDto(requireMedicine(id));
  }

  @Transactional
  public MedicineDTO create(MedicineDTO dto) {
    if (medicineRepository.existsDuplicate(
        dto.getName(), dto.getGenericName(), dto.getStrength(), dto.getForm())) {
      throw new ApiException(
          "DUPLICATE_MEDICINE",
          "Medicine with same name, generic name, strength, and form already exists",
          HttpStatus.CONFLICT);
    }
    Medicine medicine = new Medicine();
    updateFields(medicine, dto);
    medicine = medicineRepository.save(medicine);

    if (dto.getCurrentStock() != null && dto.getCurrentStock() > 0) {
      addStock(
          StockRequest.builder()
              .medicineId(medicine.getId())
              .batchNumber(dto.getBatchNumber() != null ? dto.getBatchNumber() : "INITIAL")
              .quantity(dto.getCurrentStock())
              .expiryDate(dto.getExpiryDate())
              .purchasePrice(
                  dto.getPurchasePrice() != null ? dto.getPurchasePrice() : BigDecimal.ZERO)
              .sellingPrice(dto.getSellingPrice() != null ? dto.getSellingPrice() : BigDecimal.ZERO)
              .build());
    }

    return toDto(medicine);
  }

  @Transactional
  public MedicineDTO update(UUID id, MedicineDTO dto) {
    Medicine medicine = requireMedicine(id);
    // Check for duplicate if name/strength/form changed
    if (!(medicine.getName().equalsIgnoreCase(dto.getName())
        && equalsIgnoreCase(medicine.getGenericName(), dto.getGenericName())
        && equalsIgnoreCase(medicine.getStrength(), dto.getStrength())
        && equalsIgnoreCase(medicine.getForm(), dto.getForm()))) {
      if (medicineRepository.existsDuplicate(
          dto.getName(), dto.getGenericName(), dto.getStrength(), dto.getForm())) {
        throw new ApiException(
            "DUPLICATE_MEDICINE",
            "Medicine with same name, generic name, strength, and form already exists",
            HttpStatus.CONFLICT);
      }
    }
    updateFields(medicine, dto);
    return toDto(medicineRepository.save(medicine));
  }

  @Transactional
  public void delete(UUID id) {
    medicineRepository.delete(requireMedicine(id));
  }

  @Transactional
  public MedicineImportResponse importExcel(MultipartFile file) {
    try (var workbook = WorkbookFactory.create(file.getInputStream())) {
      int totalRows = 0;
      int imported = 0;
      int skipped = 0;

      for (Row row : workbook.getSheetAt(0)) {
        if (row.getRowNum() == 0 || blank(row, 0)) {
          continue;
        }

        totalRows++;
        String name = text(row, 0);
        String genericName = text(row, 1);
        String composition = text(row, 2);
        String strength = text(row, 3);
        String form = text(row, 4);
        String manufacturer = text(row, 5);
        String scheduleType = text(row, 6);

        if (medicineRepository.existsDuplicate(
            name, genericName, strength, form)) {
          log.info("Skipping duplicate medicine: {} {} {}", name, strength, form);
          skipped++;
          continue;
        }

        Medicine medicine = new Medicine();
        medicine.setName(name);
        medicine.setGenericName(genericName);
        medicine.setComposition(composition);
        medicine.setStrength(strength);
        medicine.setForm(form);
        medicine.setManufacturer(manufacturer);
        medicine.setScheduleType(scheduleType);
        medicineRepository.save(medicine);
        imported++;
      }

      return MedicineImportResponse.builder()
          .totalRows(totalRows)
          .importedRows(imported)
          .skippedRows(skipped)
          .build();
    } catch (IOException e) {
      log.error("Excel import failed", e);
      throw new ApiException(
          "IMPORT_FAILED", "Unable to read medicine Excel file", HttpStatus.BAD_REQUEST);
    }
  }

  private boolean equalsIgnoreCase(String s1, String s2) {
    if (s1 == null && s2 == null) return true;
    if (s1 == null || s2 == null) return false;
    return s1.equalsIgnoreCase(s2);
  }

  @Transactional
  public PharmacyStockDTO addStock(StockRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Medicine medicine = requireMedicine(request.getMedicineId());
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
    return toStockDto(pharmacyStockRepository.save(stock));
  }

  @Transactional(readOnly = true)
  public PageResponse<PharmacyStockDTO> listStock(int page, int size, boolean lowStock) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PageRequest pageRequest = PageRequest.of(page, size);
    if (lowStock) {
      return PageResponse.from(
          pharmacyStockRepository
              .findAllByHospitalIdAndQuantityLessThan(hospitalId, DEFAULT_LOW_STOCK_THRESHOLD, pageRequest)
              .map(this::toStockDto));
    }
    return PageResponse.from(pharmacyStockRepository.findDetailedStock(hospitalId, pageRequest).map(this::toStockDto));
  }

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
      Medicine medicine = requireMedicine(requestedItem.getMedicineId());
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

  @Transactional(readOnly = true)
  public List<PharmacyStockDTO> expiryAlerts(int days) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    LocalDate today = LocalDate.now();
    return pharmacyStockRepository.findExpiringBatches(hospitalId, today, today.plusDays(days)).stream()
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
        .orElseThrow(() -> new ApiException("NOT_FOUND", "Medicine not found", HttpStatus.NOT_FOUND));
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

  public MedicineDTO toDto(Medicine medicine) {
    return MedicineDTO.builder()
        .id(medicine.getId())
        .name(medicine.getName())
        .genericName(medicine.getGenericName())
        .composition(medicine.getComposition())
        .strength(medicine.getStrength())
        .form(medicine.getForm())
        .scheduleType(medicine.getScheduleType())
        .category(medicine.getCategory())
        .manufacturer(medicine.getManufacturer())
        .reorderLevel(medicine.getReorderLevel())
        .purchasePrice(medicine.getPurchasePrice())
        .sellingPrice(medicine.getSellingPrice())
        .taxPercentage(medicine.getTaxPercentage())
        .temperature(medicine.getStorageTemperature())
        .humidity(medicine.getStorageHumidity())
        .specialNotes(medicine.getSpecialNotes())
        .build();
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
    medicine.setReorderLevel(dto.getReorderLevel());
    medicine.setSellingPrice(dto.getSellingPrice());
    medicine.setPurchasePrice(dto.getPurchasePrice());
    medicine.setTaxPercentage(dto.getTaxPercentage());
    medicine.setStorageTemperature(dto.getTemperature());
    medicine.setStorageHumidity(dto.getHumidity());
    medicine.setSpecialNotes(dto.getSpecialNotes());
  }

  private boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }

  private boolean blank(Row row, int cell) {
    return !hasText(text(row, cell));
  }

  private String text(Row row, int cell) {
    var value = row.getCell(cell);
    return value == null ? null : value.toString().trim();
  }
}
