package com.res.hms.pharmacy.service;

import com.res.hms.common.exception.ApiException;
import com.res.hms.domain.Medicine;
import com.res.hms.pharmacy.dto.MedicineImportResponse;
import com.res.hms.pharmacy.dto.StockImportResponse;
import com.res.hms.pharmacy.dto.StockRequest;
import com.res.hms.pharmacy.repository.MedicineRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class PharmacyImportService {

  private final MedicineRepository medicineRepository;
  private final PharmacyStockService pharmacyStockService;

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

      for (Row row : workbook.getSheetAt(0)) {
        if (row.getRowNum() == 0 || blank(row, 0)) {
          continue;
        }

        totalRows++;
        log.info("Processing row {}: name={}", row.getRowNum(), text(row, 0));
        String medicineName = text(row, 0);
        String batchNumber = text(row, 1);
        String expiryStr = text(row, 2);
        String qtyStr = text(row, 3);
        String purchasePriceStr = text(row, 4);
        String sellingPriceStr = text(row, 5);

        var medicineOpt = medicineRepository.findByNameIgnoreCase(medicineName);
        if (medicineOpt.isEmpty()) {
          log.warn("Medicine not found for stock import: {}", medicineName);
          notFound++;
          continue;
        }

        try {
          pharmacyStockService.addStock(
              StockRequest.builder()
                  .medicineId(medicineOpt.get().getId())
                  .batchNumber(batchNumber)
                  .expiryDate(expiryStr != null && !expiryStr.isBlank() ? LocalDate.parse(expiryStr) : null)
                  .quantity(qtyStr != null && !qtyStr.isBlank() ? (int) Double.parseDouble(qtyStr) : 0)
                  .purchasePrice(
                      purchasePriceStr != null && !purchasePriceStr.isBlank() ? new BigDecimal(purchasePriceStr) : BigDecimal.ZERO)
                  .sellingPrice(
                      sellingPriceStr != null && !sellingPriceStr.isBlank() ? new BigDecimal(sellingPriceStr) : BigDecimal.ZERO)
                  .build());
          imported++;
        } catch (Exception e) {
          log.error("Failed to import stock row: {}", row.getRowNum(), e);
          skipped++;
        }
      }

      return StockImportResponse.builder()
          .totalRows(totalRows)
          .importedRows(imported)
          .skippedRows(skipped)
          .medicineNotFoundRows(notFound)
          .build();
    } catch (IOException e) {
      log.error("Stock Excel import failed", e);
      throw new ApiException(
          "IMPORT_FAILED", "Unable to read stock Excel file", HttpStatus.BAD_REQUEST);
    }
  }

  private boolean blank(Row row, int cell) {
    String val = text(row, cell);
    return val == null || val.isBlank();
  }

  private String text(Row row, int cell) {
    var value = row.getCell(cell);
    return value == null ? null : value.toString().trim();
  }
}
