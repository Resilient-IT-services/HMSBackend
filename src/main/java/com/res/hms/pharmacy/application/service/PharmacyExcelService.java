package com.res.hms.pharmacy.application.service;

import static com.res.hms.pharmacy.application.service.PharmacyExcelHelper.getFeedbackFileBase64;
import static com.res.hms.pharmacy.application.service.PharmacyExcelHelper.getOrCreateCell;
import static com.res.hms.pharmacy.application.service.PharmacyExcelHelper.hasText;
import static com.res.hms.pharmacy.application.service.PharmacyExcelHelper.isBlank;
import static com.res.hms.pharmacy.application.service.PharmacyExcelHelper.prepareHeaderAndGetStatusColumn;
import static com.res.hms.pharmacy.application.service.PharmacyExcelHelper.text;
import static com.res.hms.pharmacy.application.service.PharmacyExcelHelper.updateCell;

import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.pharmacy.application.dto.MedicineImportResponse;
import com.res.hms.pharmacy.application.dto.StockImportResponse;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.domain.MedicineForm;
import com.res.hms.pharmacy.domain.PharmacyStock;
import com.res.hms.pharmacy.infrastructure.repository.HospitalMedicineCatalogRepository;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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
  private final HospitalMedicineCatalogRepository hospitalMedicineCatalogRepository;
  private final PharmacyStockRepository pharmacyStockRepository;
  private final HospitalContext hospitalContext;
  private final org.apache.poi.ss.usermodel.DataFormatter dataFormatter =
      new org.apache.poi.ss.usermodel.DataFormatter();

  public MedicineImportResponse importMedicines(MultipartFile file) {
    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);
      CellStyle[] styles = PharmacyExcelHelper.createFeedbackStyles(workbook);
      int statusColIndex = prepareHeaderAndGetStatusColumn(sheet, styles[0]);

      int totalRows = 0;
      int imported = 0;
      int skipped = 0;
      int failed = 0;

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null || isBlank(row, 0)) {
          continue;
        }
        totalRows++;
        String result = processMedicineRow(row, statusColIndex, styles);
        if ("SUCCESS".equals(result)) {
          imported++;
        } else if (result != null && result.startsWith("SKIPPED")) {
          skipped++;
        } else {
          failed++;
        }
      }
      return MedicineImportResponse.builder()
          .totalRows(totalRows)
          .importedRows(imported)
          .skippedRows(skipped)
          .failedRows(failed)
          .errorFileBase64(failed > 0 ? getFeedbackFileBase64(workbook) : null)
          .build();
    } catch (IOException e) {
      throw new ApiException("IMPORT_FAILED", "Unable to process Excel file", HttpStatus.BAD_REQUEST);
    }
  }

  @Transactional
  public StockImportResponse importStock(MultipartFile file) {
    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);
      CellStyle[] styles = PharmacyExcelHelper.createFeedbackStyles(workbook);
      int statusColIndex = prepareHeaderAndGetStatusColumn(sheet, styles[0]);

      int totalRows = 0;
      int imported = 0;
      int skipped = 0;
      int failed = 0;
      int notFound = 0;
      String lastError = null;

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null || isBlank(row, 0)) {
          continue;
        }
        totalRows++;
        String result = processStockRow(row, statusColIndex, styles);
        if ("SUCCESS".equals(result)) {
          imported++;
        } else if ("SKIPPED".equals(result)) {
          skipped++;
        } else if ("NOT_FOUND".equals(result)) {
          notFound++;
        } else {
          failed++;
          lastError = result;
        }
      }
      return StockImportResponse.builder()
          .totalRows(totalRows)
          .importedRows(imported)
          .skippedRows(skipped)
          .medicineNotFoundRows(notFound)
          .lastError(lastError)
          .errorFileBase64(
              (failed > 0 || skipped > 0 || notFound > 0) ? getFeedbackFileBase64(workbook) : null)
          .build();
    } catch (IOException e) {
      throw new ApiException(
          "IMPORT_FAILED", "Unable to read stock Excel file", HttpStatus.BAD_REQUEST);
    }
  }

  private String processStockRow(Row row, int statusCol, CellStyle[] styles) {
    Cell statusCell = getOrCreateCell(row, statusCol);
    try {
      String nameRaw = text(row, 0);
      String batch = text(row, 1);
      String expStr = text(row, 2);
      String qtyStr = text(row, 3);
      String pPriceStr = text(row, 4);
      String sPriceStr = text(row, 5);
      String reorderLevelStr = row.getLastCellNum() > 6 ? text(row, 6) : null;

      String name = nameRaw == null ? "" : nameRaw.replaceAll("[\\p{Z}\\s]+", " ").trim();

      Medicine medicine = findMedicine(name, statusCell, styles[3]);
      if (medicine == null) {
        return "NOT_FOUND";
      }

      List<String> errors =
          PharmacyExcelHelper.validateStockData(name, batch, qtyStr, sPriceStr, pPriceStr, expStr);
      if (!errors.isEmpty()) {
        throw new IllegalArgumentException(String.join(" | ", errors));
      }

      UUID hospitalId = hospitalContext.requireHospitalId();
      if (pharmacyStockRepository
          .findByHospitalIdAndMedicineIdAndBatchNumber(hospitalId, medicine.getId(), batch)
          .isPresent()) {
        updateCell(statusCell, "SKIPPED: Batch already exists", styles[2]);
        return "SKIPPED";
      }

      Integer reorderLevel = parseInteger(reorderLevelStr);
      updateMedicineReorderLevel(medicine.getId(), reorderLevel);

      saveStock(hospitalId, medicine, batch, parseDate(row, 2, expStr), qtyStr, pPriceStr, sPriceStr);
      updateCell(statusCell, "SUCCESS", styles[1]);
      return "SUCCESS";
    } catch (Exception e) {
      updateCell(statusCell, "ERROR: " + e.getMessage(), styles[3]);
      return "ERROR";
    }
  }

  private String processMedicineRow(Row row, int statusCol, CellStyle[] styles) {
    Cell statusCell = getOrCreateCell(row, statusCol);
    try {
      String name = text(row, 0);
      String gName = text(row, 1);
      String comp = text(row, 2);
      String str = text(row, 3);
      String fStr = text(row, 4);
      String mf = text(row, 5);
      String rlStr = text(row, 6);

      List<String> errors =
          PharmacyExcelHelper.validateMedicineData(name, gName, comp, str, fStr, mf);
      if (!errors.isEmpty()) {
        throw new IllegalArgumentException(String.join(" | ", errors));
      }

      MedicineForm form = PharmacyExcelHelper.parseMedicineForm(fStr);
      if (medicineRepository.existsDuplicate(name, gName, str, form)) {
        updateCell(statusCell, "SKIPPED: Medicine already exists", styles[2]);
        return "SKIPPED";
      }

      Medicine m = new Medicine();
      m.setName(name);
      m.setGenericName(gName);
      m.setComposition(comp);
      m.setStrength(str);
      m.setForm(form);
      m.setManufacturer(mf);
      m.setScheduleType(com.res.hms.pharmacy.domain.MedicineSchedule.OTC);
      medicineRepository.save(m);

      updateMedicineReorderLevel(m.getId(), parseInteger(rlStr));
      updateCell(statusCell, "SUCCESS", styles[1]);
      return "SUCCESS";
    } catch (Exception e) {
      updateCell(statusCell, "ERROR: " + e.getMessage(), styles[3]);
      return "ERROR";
    }
  }

  private void saveStock(
      UUID hId, Medicine m, String b, LocalDate exp, String q, String p, String s) {
    PharmacyStock stock =
        pharmacyStockRepository
            .findByHospitalIdAndMedicineIdAndBatchNumber(hId, m.getId(), b)
            .orElseGet(
                () -> {
                  PharmacyStock ps = new PharmacyStock();
                  ps.setHospitalId(hId);
                  ps.setMedicine(m);
                  ps.setBatchNumber(b);
                  ps.setQuantity(0);
                  return ps;
                });
    stock.setExpiryDate(exp);

    // Use robust parsing to avoid NUMERIC/STRING cell mismatch errors
    int qtyToAdd = 0;
    if (hasText(q)) {
      try {
        qtyToAdd = (int) Double.parseDouble(q.replaceAll("[^\\d.]", ""));
      } catch (Exception e) {
        log.warn("Invalid quantity format in saveStock: {}", q);
      }
    }

    stock.setQuantity(stock.getQuantity() + qtyToAdd);

    try {
      stock.setPurchasePrice(
          hasText(p) ? new BigDecimal(p.replaceAll("[^\\d.]", "")) : BigDecimal.ZERO);
      stock.setSellingPrice(
          hasText(s) ? new BigDecimal(s.replaceAll("[^\\d.]", "")) : BigDecimal.ZERO);
    } catch (Exception e) {
      log.warn("Invalid price format in saveStock: P={}, S={}", p, s);
    }

    pharmacyStockRepository.save(stock);
  }

  private Medicine findMedicine(String name, Cell cell, CellStyle style) {
    return medicineRepository
        .findByExactName(name)
        .stream()
        .findFirst()
        .or(() -> medicineRepository.findByNameIgnoreCase(name))
        .orElseGet(
            () -> {
              updateCell(cell, "ERROR: Medicine '" + name + "' not found", style);
              return null;
            });
  }

  private void updateMedicineReorderLevel(UUID mId, Integer rl) {
    Optional.ofNullable(hospitalContext.getHospitalIdOrNull())
        .ifPresent(
            hId -> {
              var cat =
                  hospitalMedicineCatalogRepository
                      .findByHospitalIdAndMedicineId(hId, mId)
                      .orElseGet(
                          () -> {
                            var c = new com.res.hms.pharmacy.domain.HospitalMedicineCatalog();
                            c.setHospitalId(hId);
                            c.setMedicine(medicineRepository.getReferenceById(mId));
                            return c;
                          });
              cat.setReorderLevel(rl != null ? rl : 50);
              hospitalMedicineCatalogRepository.save(cat);
            });
  }

  private LocalDate parseDate(Row row, int col, String val) {
    if (!hasText(val)) {
      return null;
    }
    Cell c = row.getCell(col);
    if (c != null && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(c)) {
      return c.getLocalDateTimeCellValue().toLocalDate();
    }
    String s = val.replaceAll("[^\\d\\-/.]", "").trim();
    try {
      return LocalDate.parse(s);
    } catch (Exception e) {
      // Ignored
    }
    try {
      return LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    } catch (Exception e) {
      // Ignored
    }
    try {
      return LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    } catch (Exception e) {
      // Ignored
    }
    try {
      return LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    } catch (Exception e) {
      // Ignored
    }
    return null;
  }

  private Integer parseInteger(String s) {
    try {
      return hasText(s) ? (int) Double.parseDouble(s) : null;
    } catch (Exception e) {
      return null;
    }
  }
}
