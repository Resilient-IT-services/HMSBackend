package com.res.hms.pharmacy.application.service;

import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.pharmacy.application.dto.MedicineImportResponse;
import com.res.hms.pharmacy.application.dto.StockImportResponse;
import com.res.hms.pharmacy.application.dto.StockRequest;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.domain.PharmacyStock;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class PharmacyExcelService {

  private final MedicineRepository medicineRepository;
  private final PharmacyStockRepository pharmacyStockRepository;
  private final HospitalContext hospitalContext;
  private final org.apache.poi.ss.usermodel.DataFormatter dataFormatter =
      new org.apache.poi.ss.usermodel.DataFormatter();

  @Transactional
  public MedicineImportResponse importMedicines(MultipartFile file) {
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

        if (medicineRepository.existsDuplicate(name, genericName, strength, form)) {
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

  @Transactional
  public StockImportResponse importStock(MultipartFile file) {
    try (var workbook = WorkbookFactory.create(file.getInputStream())) {
      int totalRows = 0;
      int imported = 0;
      int skipped = 0;
      int notFound = 0;
      String lastError = null;

      for (Row row : workbook.getSheetAt(0)) {
        if (row.getRowNum() == 0 || blank(row, 0)) {
          continue;
        }

        totalRows++;
        String medicineName = text(row, 0);
        String batchNumber = text(row, 1);
        String expiryStr = text(row, 2);
        String qtyStr = text(row, 3);
        String purchasePriceStr = text(row, 4);
        String sellingPriceStr = text(row, 5);

        var medicines = medicineRepository.findByExactName(medicineName);
        if (medicines.isEmpty()) {
          log.warn("Medicine not found for stock import: {}", medicineName);
          notFound++;
          continue;
        }
        var medicine = medicines.get(0);

        try {
          LocalDate expiryDate = null;
          if (hasText(expiryStr)) {
            try {
              expiryDate = LocalDate.parse(expiryStr);
            } catch (Exception e) {
              log.warn("Invalid date format in Excel row {}: {}", row.getRowNum(), expiryStr);
            }
          }

          // Inlined addStock logic to avoid circular dependency
          addStockInternal(
              StockRequest.builder()
                  .medicineId(medicine.getId())
                  .batchNumber(batchNumber)
                  .expiryDate(expiryDate)
                  .quantity(qtyStr != null ? (int) Double.parseDouble(qtyStr) : 0)
                  .purchasePrice(
                      purchasePriceStr != null ? new BigDecimal(purchasePriceStr) : BigDecimal.ZERO)
                  .sellingPrice(
                      sellingPriceStr != null ? new BigDecimal(sellingPriceStr) : BigDecimal.ZERO)
                  .build());
          imported++;
        } catch (Exception e) {
          log.error("Failed to import stock row {}: {}", row.getRowNum(), e.getMessage());
          lastError = e.getMessage();
          skipped++;
        }
      }

      return StockImportResponse.builder()
          .totalRows(totalRows)
          .importedRows(imported)
          .skippedRows(skipped)
          .medicineNotFoundRows(notFound)
          .lastError(lastError)
          .build();
    } catch (IOException e) {
      log.error("Stock Excel import failed", e);
      throw new ApiException(
          "IMPORT_FAILED", "Unable to read stock Excel file", HttpStatus.BAD_REQUEST);
    }
  }

  private void addStockInternal(StockRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Medicine medicine =
        medicineRepository
            .findById(request.getMedicineId())
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Medicine not found", HttpStatus.NOT_FOUND));

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
    pharmacyStockRepository.save(stock);
  }

  private boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }

  private boolean blank(Row row, int cell) {
    return !hasText(text(row, cell));
  }

  private String text(Row row, int cell) {
    var value = row.getCell(cell);
    return value == null ? null : dataFormatter.formatCellValue(value).trim();
  }
}
