package com.res.hms.pharmacy.application.service;

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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private final jakarta.persistence.EntityManager entityManager;

  public MedicineImportResponse importMedicines(MultipartFile file) {
    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);
      CellStyle[] styles = PharmacyExcelHelper.createFeedbackStyles(workbook);
      int statusColIndex = PharmacyExcelHelper.prepareHeaderAndGetStatusColumn(sheet, styles[0]);
      int totalRows = 0;
      int imported = 0;
      int skipped = 0;
      int failed = 0;

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null || PharmacyExcelHelper.isBlank(row, 0)) {
          continue;
        }
        totalRows++;
        String result = processMedicineRow(row, statusColIndex, styles);
        entityManager.flush();
        entityManager.clear();
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
          .errorFileBase64(failed > 0 ? PharmacyExcelHelper.getFeedbackFileBase64(workbook) : null)
          .build();
    } catch (IOException e) {
      throw new ApiException(
          "IMPORT_FAILED", "Unable to process Excel file", HttpStatus.BAD_REQUEST);
    }
  }

  @Transactional
  public StockImportResponse importStock(MultipartFile file) {
    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);
      CellStyle[] styles = PharmacyExcelHelper.createFeedbackStyles(workbook);
      int statusColIndex = PharmacyExcelHelper.prepareHeaderAndGetStatusColumn(sheet, styles[0]);
      int totalRows = 0;
      int imported = 0;
      int skipped = 0;
      int failed = 0;
      int notFound = 0;
      String lastError = null;
      Map<String, UUID> processedBatches = new HashMap<>();

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null || PharmacyExcelHelper.isBlank(row, 0)) {
          continue;
        }
        totalRows++;
        String result = processStockRow(row, statusColIndex, styles, processedBatches);
        entityManager.flush();
        entityManager.clear();
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
          .failedRows(failed)
          .medicineNotFoundRows(notFound)
          .lastError(lastError)
          .errorFileBase64(
              (failed > 0 || skipped > 0 || notFound > 0)
                  ? PharmacyExcelHelper.getFeedbackFileBase64(workbook)
                  : null)
          .build();
    } catch (IOException e) {
      throw new ApiException(
          "IMPORT_FAILED", "Unable to read stock Excel file", HttpStatus.BAD_REQUEST);
    }
  }

  private String processStockRow(
      Row row, int statusCol, CellStyle[] styles, Map<String, UUID> batches) {
    Cell statusCell = PharmacyExcelHelper.getOrCreateCell(row, statusCol);
    try {
      String name =
          Optional.ofNullable(PharmacyExcelHelper.text(row, 0))
              .map(s -> s.replaceAll("[\\p{Z}\\s]+", " ").trim())
              .orElse("");
      String batch =
          Optional.ofNullable(PharmacyExcelHelper.text(row, 1))
              .map(s -> s.trim().toUpperCase())
              .orElse("");
      String expStr = PharmacyExcelHelper.text(row, 2);
      String qtyStr = PharmacyExcelHelper.text(row, 3);
      String pPriceStr = PharmacyExcelHelper.text(row, 4);
      String sPriceStr = PharmacyExcelHelper.text(row, 5);

      Medicine medicine = findMedicine(name, statusCell, styles[3]);
      if (medicine == null) {
        return "NOT_FOUND";
      }

      List<String> errors =
          PharmacyExcelHelper.validateStockData(name, batch, qtyStr, sPriceStr, pPriceStr, expStr);
      if (!errors.isEmpty()) {
        throw new IllegalArgumentException(String.join(" | ", errors));
      }

      if (batches.containsKey(batch)) {
        if (!batches.get(batch).equals(medicine.getId())) {
          throw new IllegalArgumentException(
              "Duplicate Error: Batch '"
                  + batch
                  + "' is already assigned to another medicine in this file");
        }
        PharmacyExcelHelper.updateCell(statusCell, "SKIPPED: Batch already exists", styles[2]);
        return "SKIPPED";
      }

      UUID hId = hospitalContext.requireHospitalId();
      if (pharmacyStockRepository.findByHospitalIdAndBatchNumber(hId, batch).isPresent()) {
        PharmacyExcelHelper.updateCell(statusCell, "SKIPPED: Batch already exists", styles[2]);
        return "SKIPPED";
      }

      batches.put(batch, medicine.getId());
      LocalDate expiryDate = PharmacyExcelHelper.parseDate(expStr);
      if (expiryDate == null) {
        throw new IllegalArgumentException(
            "[Expiry Date] Invalid format: '" + expStr + "'. Use YYYY-MM-DD");
      }

      saveStock(hId, medicine, batch, expiryDate, qtyStr, pPriceStr, sPriceStr);
      PharmacyExcelHelper.updateCell(statusCell, "SUCCESS", styles[1]);
      return "SUCCESS";
    } catch (Exception e) {
      PharmacyExcelHelper.updateCell(statusCell, "ERROR: " + getErrorMessage(e), styles[3]);
      return "ERROR";
    }
  }

  private String processMedicineRow(Row row, int statusCol, CellStyle[] styles) {
    Cell statusCell = PharmacyExcelHelper.getOrCreateCell(row, statusCol);
    try {
      String name = PharmacyExcelHelper.text(row, 0);
      String gName = PharmacyExcelHelper.text(row, 1);
      String comp = PharmacyExcelHelper.text(row, 2);
      String str = PharmacyExcelHelper.text(row, 3);
      String fStr = PharmacyExcelHelper.text(row, 4);
      String mf = PharmacyExcelHelper.text(row, 5);
      String thresholdStr = PharmacyExcelHelper.text(row, 7);
      List<String> errors =
          PharmacyExcelHelper.validateMedicineData(name, gName, comp, str, fStr, mf);
      if (!errors.isEmpty()) {
        throw new IllegalArgumentException(String.join(" | ", errors));
      }

      MedicineForm form = PharmacyExcelHelper.parseMedicineForm(fStr);
      if (medicineRepository.existsDuplicate(name, gName, str, form)) {
        throw new IllegalArgumentException("Duplicate: Medicine '" + name + "' already exists");
      }

      Medicine m = new Medicine();
      m.setName(name);
      m.setGenericName(gName);
      m.setComposition(comp);
      m.setStrength(str);
      m.setForm(form);
      m.setManufacturer(mf);
      m.setScheduleType(com.res.hms.pharmacy.domain.MedicineSchedule.OTC);
      m.setThresholdQuantity(PharmacyExcelHelper.parsAnInt(thresholdStr));
      medicineRepository.save(m);

      PharmacyExcelHelper.updateCell(statusCell, "SUCCESS", styles[1]);
      return "SUCCESS";
    } catch (Exception e) {
      PharmacyExcelHelper.updateCell(statusCell, "ERROR: " + getErrorMessage(e), styles[3]);
      return "ERROR";
    }
  }

  private String getErrorMessage(Exception e) {
    Throwable root = e;
    while (root.getCause() != null) {
      root = root.getCause();
    }
    String msg = root.getMessage();
    if (msg == null || msg.isBlank()) {
      msg = e.getMessage();
    }
    return (msg != null && msg.length() > 150) ? msg.substring(0, 150) : msg;
  }

  private void saveStock(
      UUID hId, Medicine m, String b, LocalDate exp, String q, String p, String s) {
    String batch = b.trim().toUpperCase();
    PharmacyStock stock =
        pharmacyStockRepository
            .findByHospitalIdAndMedicineIdAndBatchNumber(hId, m.getId(), batch)
            .orElseGet(
                () -> {
                  PharmacyStock ps = new PharmacyStock();
                  ps.setHospitalId(hId);
                  ps.setMedicine(m);
                  ps.setBatchNumber(batch);
                  ps.setQuantity(0);
                  return ps;
                });
    stock.setExpiryDate(exp);
    stock.setQuantity(stock.getQuantity() + PharmacyExcelHelper.parseQuantity(q));
    stock.setPurchasePrice(PharmacyExcelHelper.parseBigDecimal(p));
    stock.setSellingPrice(PharmacyExcelHelper.parseBigDecimal(s));
    pharmacyStockRepository.saveAndFlush(stock);
  }

  private Medicine findMedicine(String name, Cell cell, CellStyle style) {
    return medicineRepository.findByExactName(name).stream()
        .findFirst()
        .or(() -> medicineRepository.findByNameIgnoreCase(name))
        .orElseGet(
            () -> {
              PharmacyExcelHelper.updateCell(
                  cell, "ERROR: Medicine '" + name + "' not found", style);
              return null;
            });
  }
}
