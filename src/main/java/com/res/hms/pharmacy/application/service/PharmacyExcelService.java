package com.res.hms.pharmacy.application.service;

import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.pharmacy.application.dto.MedicineImportResponse;
import com.res.hms.pharmacy.application.dto.StockImportResponse;
import com.res.hms.pharmacy.application.dto.StockRequest;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.domain.MedicineForm;
import com.res.hms.pharmacy.domain.MedicineSchedule;
import com.res.hms.pharmacy.domain.PharmacyStock;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
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
  private final PharmacyStockRepository pharmacyStockRepository;
  private final HospitalContext hospitalContext;
  private final org.apache.poi.ss.usermodel.DataFormatter dataFormatter =
      new org.apache.poi.ss.usermodel.DataFormatter();

  /**
   * Imports medicines from an Excel file.
   * Operates on a single workbook to provide feedback on each row.
   */
  public MedicineImportResponse importMedicines(MultipartFile file) {
    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);

      // Prepare styles
      CellStyle headerStyle = createStyle(workbook, IndexedColors.GREY_25_PERCENT, FillPatternType.SOLID_FOREGROUND, true);
      CellStyle successStyle = createStyle(workbook, IndexedColors.LIGHT_GREEN, FillPatternType.SOLID_FOREGROUND, false);
      CellStyle skippedStyle = createStyle(workbook, IndexedColors.LIGHT_YELLOW, FillPatternType.SOLID_FOREGROUND, false);
      CellStyle errorStyle = createStyle(workbook, IndexedColors.RED, FillPatternType.SOLID_FOREGROUND, false);

      int statusColIndex = prepareHeaderAndGetStatusColumn(sheet, headerStyle);

      int totalRows = 0;
      int imported = 0;
      int skipped = 0;
      int failed = 0;

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null || blank(row, 0)) {
          continue;
        }

        totalRows++;
        String result = processMedicineRow(row, statusColIndex, successStyle, skippedStyle, errorStyle);
        if ("SUCCESS".equals(result)) {
          imported++;
        } else if (result != null && result.startsWith("SKIPPED")) {
          skipped++;
        } else {
          failed++;
        }
      }

      String base64 = null;
      // Generate feedback file ONLY if there are actual failures (failed > 0)
      if (failed > 0) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
      }

      return MedicineImportResponse.builder()
          .totalRows(totalRows)
          .importedRows(imported)
          .skippedRows(skipped)
          .failedRows(failed)
          .errorFileBase64(base64)
          .build();

    } catch (IOException e) {
      log.error("Excel import failed", e);
      throw new ApiException("IMPORT_FAILED", "Unable to process Excel file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
    }
  }

  private int prepareHeaderAndGetStatusColumn(Sheet sheet, CellStyle headerStyle) {
    Row headerRow = sheet.getRow(0);
    if (headerRow == null) {
      headerRow = sheet.createRow(0);
    }

    int statusColIndex = -1;
    for (int c = 0; c < headerRow.getLastCellNum(); c++) {
      String headerVal = text(headerRow, c);
      if ("IMPORT STATUS".equalsIgnoreCase(headerVal)) {
        statusColIndex = c;
        break;
      }
    }

    if (statusColIndex == -1) {
      statusColIndex = headerRow.getLastCellNum();
      Cell statusHeaderCell = headerRow.createCell(statusColIndex);
      statusHeaderCell.setCellValue("IMPORT STATUS");
      statusHeaderCell.setCellStyle(headerStyle);
    } else {
      headerRow.getCell(statusColIndex).setCellStyle(headerStyle);
    }
    return statusColIndex;
  }

  private String processMedicineRow(Row row, int statusColIndex, CellStyle successStyle,
                                   CellStyle skippedStyle, CellStyle errorStyle) {
    Cell statusCell = row.getCell(statusColIndex);
    if (statusCell == null) {
      statusCell = row.createCell(statusColIndex);
    }
    statusCell.setCellValue("");

    try {
      String name = text(row, 0);
      String genericName = text(row, 1);
      String composition = text(row, 2);
      String strength = text(row, 3);
      String formStr = text(row, 4);
      String manufacturer = text(row, 5);
      String scheduleStr = text(row, 6);

      List<String> errors = validateMedicineData(name, genericName, composition, strength, formStr, manufacturer, scheduleStr);
      if (!errors.isEmpty()) {
        throw new IllegalArgumentException(String.join(" | ", errors));
      }

      MedicineForm form = parseMedicineForm(formStr);
      if (medicineRepository.existsDuplicate(name, genericName, strength, form)) {
        statusCell.setCellValue("SKIPPED: Medicine already exists");
        statusCell.setCellStyle(skippedStyle);
        return "SKIPPED";
      }

      Medicine medicine = new Medicine();
      medicine.setName(name);
      medicine.setGenericName(genericName);
      medicine.setComposition(composition);
      medicine.setStrength(strength);
      medicine.setForm(form);
      medicine.setManufacturer(manufacturer);
      medicine.setScheduleType(parseMedicineSchedule(scheduleStr));
      medicineRepository.save(medicine);

      statusCell.setCellValue("SUCCESS");
      statusCell.setCellStyle(successStyle);
      return "SUCCESS";

    } catch (Exception e) {
      String errorMsg = "ERROR: " + e.getMessage();
      statusCell.setCellValue(errorMsg);
      statusCell.setCellStyle(errorStyle);
      return "ERROR";
    }
  }

  private List<String> validateMedicineData(String name, String genericName, String composition, String strength, String formStr, String manufacturer, String scheduleStr) {
    List<String> errors = new ArrayList<>();
    if (!hasText(name)) {
      errors.add("Name is required");
    }
    if (!hasText(genericName)) {
      errors.add("Generic Name is required");
    }
    if (!hasText(composition)) {
      errors.add("Salt/Composition is required");
    }
    if (!hasText(strength)) {
      errors.add("Strength is required");
    }
    if (!hasText(manufacturer)) {
      errors.add("Manufacturer is required");
    }

    MedicineForm form = parseMedicineForm(formStr);
    if (form == null) {
      String acceptedForms = java.util.Arrays.toString(MedicineForm.values());
      if (hasText(formStr)) {
        errors.add("Invalid Medicine Form: " + formStr + ". Accepted: " + acceptedForms);
      } else {
        errors.add("Medicine Form is required. Accepted: " + acceptedForms);
      }
    }

    MedicineSchedule schedule = parseMedicineSchedule(scheduleStr);
    if (schedule == null && hasText(scheduleStr)) {
      String acceptedSchedules = java.util.Arrays.toString(MedicineSchedule.values());
      errors.add("Invalid Schedule: " + scheduleStr + ". Accepted: " + acceptedSchedules);
    }
    return errors;
  }

  private MedicineForm parseMedicineForm(String val) {
    if (!hasText(val)) {
      return null;
    }
    String s = val.trim().toUpperCase();
    try {
      return MedicineForm.valueOf(s);
    } catch (Exception e) {
      return PharmacyExcelHelper.mapFormFallback(s);
    }
  }

  private MedicineSchedule parseMedicineSchedule(String val) {
    if (!hasText(val)) {
      return MedicineSchedule.OTC;
    }
    String s = val.trim().toUpperCase();
    try {
      MedicineSchedule mapped = PharmacyExcelHelper.mapScheduleFallback(s);
      return mapped != null ? mapped : MedicineSchedule.valueOf(s);
    } catch (Exception e) {
      return null;
    }
  }

  private CellStyle createStyle(Workbook wb, IndexedColors color, FillPatternType pattern, boolean bold) {
    CellStyle style = wb.createCellStyle();
    style.setFillForegroundColor(color.getIndex());
    style.setFillPattern(pattern);
    if (bold) {
      org.apache.poi.ss.usermodel.Font font = wb.createFont();
      font.setBold(true);
      style.setFont(font);
    }
    return style;
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
        try {
          String result = processStockRow(row);
          if ("SUCCESS".equals(result)) {
            imported++;
          } else if ("NOT_FOUND".equals(result)) {
            notFound++;
          }
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

  private String processStockRow(Row row) {
    String medicineName = text(row, 0);
    String batchNumber = text(row, 1);
    String expiryStr = text(row, 2);
    String qtyStr = text(row, 3);
    String purchasePriceStr = text(row, 4);
    String sellingPriceStr = text(row, 5);

    var medicines = medicineRepository.findByExactName(medicineName);
    if (medicines.isEmpty()) {
      log.warn("Medicine not found for stock import: {}", medicineName);
      return "NOT_FOUND";
    }
    var medicine = medicines.get(0);

    LocalDate expiryDate = null;
    if (hasText(expiryStr)) {
      try {
        expiryDate = LocalDate.parse(expiryStr);
      } catch (Exception e) {
        log.warn("Invalid date format in Excel row {}: {}", row.getRowNum(), expiryStr);
      }
    }

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
    return "SUCCESS";
  }

  private void addStockInternal(StockRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Medicine medicine = medicineRepository.findById(request.getMedicineId())
            .orElseThrow(() -> new ApiException("NOT_FOUND", "Medicine not found", HttpStatus.NOT_FOUND));

    PharmacyStock stock = pharmacyStockRepository
            .findByHospitalIdAndMedicineIdAndBatchNumber(hospitalId, request.getMedicineId(), request.getBatchNumber())
            .orElseGet(() -> createNewStock(hospitalId, medicine, request.getBatchNumber()));

    stock.setExpiryDate(request.getExpiryDate());
    stock.setQuantity(stock.getQuantity() + request.getQuantity());
    stock.setPurchasePrice(request.getPurchasePrice());
    stock.setSellingPrice(request.getSellingPrice());
    pharmacyStockRepository.save(stock);
  }

  private PharmacyStock createNewStock(UUID hospitalId, Medicine medicine, String batchNumber) {
    PharmacyStock s = new PharmacyStock();
    s.setHospitalId(hospitalId);
    s.setMedicine(medicine);
    s.setBatchNumber(batchNumber);
    s.setQuantity(0);
    return s;
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
