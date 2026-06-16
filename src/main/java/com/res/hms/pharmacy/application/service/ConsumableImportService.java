package com.res.hms.pharmacy.application.service;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.pharmacy.application.dto.ConsumableImportDto;
import com.res.hms.pharmacy.application.dto.ImportResponseDto;
import com.res.hms.pharmacy.domain.DepartmentType;
import com.res.hms.pharmacy.domain.HospitalMedicineCatalog;
import com.res.hms.pharmacy.domain.ItemType;
import com.res.hms.pharmacy.domain.Medicine;
import com.res.hms.pharmacy.infrastructure.repository.HospitalMedicineCatalogRepository;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumableImportService {

  private final MedicineRepository repository;
  private final HospitalMedicineCatalogRepository hospitalMedicineCatalogRepository;
  private final HospitalContext hospitalContext;

  @Transactional
  public ImportResponseDto importConsumables(MultipartFile file) {
    try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);
      CellStyle[] styles = PharmacyExcelHelper.createFeedbackStyles(workbook);
      int statusColIndex = PharmacyExcelHelper.prepareHeaderAndGetStatusColumn(sheet, styles[0]);

      // 1. Pre-load Cache (Case-Insensitive & Global)
      Set<String> dbCodes =
          repository.findAllItemCodes().stream()
              .map(String::toUpperCase)
              .collect(Collectors.toSet());
      Set<String> dbDeptNames =
          repository.findAllDeptItemNames().stream()
              .map(String::toUpperCase)
              .collect(Collectors.toSet());
      Set<String> excelSeenCodes = new HashSet<>();

      int totalRows = 0;
      int successCount = 0;
      int failureCount = 0;

      UUID currentHospitalId = hospitalContext.getHospitalIdOrNull();

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null || PharmacyExcelHelper.isRowEmpty(row)) {
          continue;
        }

        totalRows++;
        ConsumableImportDto dto = parseRow(row);
        validate(dto, dbCodes, dbDeptNames, excelSeenCodes);

        Cell statusCell = PharmacyExcelHelper.getOrCreateCell(row, statusColIndex);

        if (dto.getErrors().isEmpty()) {
          try {
            saveItem(dto, currentHospitalId);

            // Update Cache
            dbCodes.add(dto.getItemCode().toUpperCase());
            dbDeptNames.add(
                dto.getDepartment().toUpperCase() + "_" + dto.getItemName().toUpperCase());

            successCount++;
            PharmacyExcelHelper.updateCell(statusCell, "SUCCESS", styles[1]);
          } catch (Exception e) {
            failureCount++;
            PharmacyExcelHelper.updateCell(statusCell, "ERROR: " + e.getMessage(), styles[3]);
          }
        } else {
          failureCount++;
          PharmacyExcelHelper.updateCell(statusCell, "ERROR: " + dto.getErrorLog(), styles[3]);
        }
      }

      String errorFileBase64 =
          failureCount > 0 ? PharmacyExcelHelper.getFeedbackFileBase64(workbook) : null;

      return ImportResponseDto.builder()
          .totalRows(totalRows)
          .successCount(successCount)
          .failureCount(failureCount)
          .errorFileAvailable(failureCount > 0)
          .errorFileBase64(errorFileBase64)
          .build();

    } catch (Exception e) {
      log.error("Consumable import failed", e);
      throw new RuntimeException("Failed to process Excel file: " + e.getMessage());
    }
  }

  private ConsumableImportDto parseRow(Row row) {
    ConsumableImportDto dto = new ConsumableImportDto();
    dto.setDepartment(PharmacyExcelHelper.text(row, 0));
    dto.setCategory(PharmacyExcelHelper.text(row, 1));
    dto.setItemCode(PharmacyExcelHelper.text(row, 2));
    dto.setItemName(PharmacyExcelHelper.text(row, 3));
    dto.setBaseUnit(PharmacyExcelHelper.text(row, 4));
    dto.setManufacturer(PharmacyExcelHelper.text(row, 5));
    dto.setLowStockAlert(PharmacyExcelHelper.text(row, 6));
    dto.setIsBatchTracked(PharmacyExcelHelper.text(row, 7));
    dto.setIsExpiryTracked(PharmacyExcelHelper.text(row, 8));
    sanitize(dto);
    return dto;
  }

  private void validate(
      ConsumableImportDto row, Set<String> codes, Set<String> deptNames, Set<String> excelSeen) {
    if (isBlank(row.getDepartment())) {
      row.addError("Department is mandatory");
    }
    if (isBlank(row.getCategory())) {
      row.addError("Category is mandatory");
    }
    if (isBlank(row.getItemCode())) {
      row.addError("Item Code is mandatory");
    }
    if (isBlank(row.getItemName())) {
      row.addError("Item Name is mandatory");
    }
    if (isBlank(row.getBaseUnit())) {
      row.addError("Base Unit is mandatory");
    }
    if (isBlank(row.getManufacturer())) {
      row.addError("Manufacturer is mandatory");
    }

    if (!row.getErrors().isEmpty()) {
      return;
    }

    try {
      DepartmentType.valueOf(row.getDepartment().toUpperCase());
    } catch (Exception e) {
      row.addError("Invalid Department. Allowed: PHARMACY, LABORATORY");
    }

    if (!excelSeen.add(row.getItemCode().toUpperCase())) {
      row.addError("Duplicate Item Code in file");
    }

    if (codes.contains(row.getItemCode().toUpperCase())) {
      row.addError("Item Code already exists");
    }

    try {
      String deptKey = row.getDepartment().toUpperCase() + "_" + row.getItemName().toUpperCase();
      if (deptNames.contains(deptKey)) {
        row.addError("Item already exists in selected Department");
      }
    } catch (Exception e) {
      // Skip if dept is invalid
    }
  }

  private void saveItem(ConsumableImportDto row, UUID hospitalId) {
    Medicine item = new Medicine();
    item.setItemType(ItemType.CONSUMABLE);
    item.setDepartment(DepartmentType.valueOf(row.getDepartment().toUpperCase()));
    item.setName(row.getItemName().toLowerCase());
    item.setItemCode(row.getItemCode().toLowerCase());
    item.setCategory(row.getCategory().toLowerCase());
    item.setBaseUnit(row.getBaseUnit().toLowerCase());
    item.setManufacturer(row.getManufacturer().toLowerCase());
    item.setThresholdQuantity(parseSafeInt(row.getLowStockAlert()));
    item.setIsBatchTracked("YES".equalsIgnoreCase(row.getIsBatchTracked()));
    item.setIsExpiryTracked("YES".equalsIgnoreCase(row.getIsExpiryTracked()));

    Medicine saved = repository.save(item);

    if (hospitalId != null) {
      HospitalMedicineCatalog catalog = new HospitalMedicineCatalog();
      catalog.setHospitalId(hospitalId);
      catalog.setMedicine(saved);
      hospitalMedicineCatalogRepository.save(catalog);
    }
  }

  private void sanitize(ConsumableImportDto d) {
    d.setDepartment(trim(d.getDepartment()));
    d.setCategory(trim(d.getCategory()));
    d.setItemCode(trim(d.getItemCode()));
    d.setItemName(trim(d.getItemName()));
    d.setBaseUnit(trim(d.getBaseUnit()));
    d.setManufacturer(trim(d.getManufacturer()));
    d.setLowStockAlert(trim(d.getLowStockAlert()));
    d.setIsBatchTracked(trim(d.getIsBatchTracked()));
    d.setIsExpiryTracked(trim(d.getIsExpiryTracked()));
  }

  private String trim(String s) {
    return s == null ? "" : s.trim();
  }

  private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private Integer parseSafeInt(String s) {
    if (s == null) {
      return 0;
    }
    try {
      return Integer.parseInt(s.replaceAll("[^0-9]", ""));
    } catch (Exception e) {
      return 0;
    }
  }
}
