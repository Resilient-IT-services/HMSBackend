package com.res.hms.pharmacy.infrastructure;

import com.res.hms.pharmacy.application.dto.ConsumableImportDto;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ConsumableExcelParser {

  public List<ConsumableImportDto> parse(MultipartFile file) {
    List<ConsumableImportDto> list = new ArrayList<>();
    try (InputStream is = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(is)) {
      Sheet sheet = workbook.getSheetAt(0);
      DataFormatter formatter = new DataFormatter();

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null || isRowEmpty(row)) {
          continue;
        }

        ConsumableImportDto dto = new ConsumableImportDto();
        dto.setRowNumber(i + 1);
        dto.setDepartment(getVal(row, 0, formatter));
        dto.setCategory(getVal(row, 1, formatter));
        dto.setItemCode(getVal(row, 2, formatter));
        dto.setItemName(getVal(row, 3, formatter));
        dto.setBaseUnit(getVal(row, 4, formatter));
        dto.setManufacturer(getVal(row, 5, formatter));
        dto.setLowStockAlert(getVal(row, 6, formatter));
        dto.setIsBatchTracked(getVal(row, 7, formatter));
        dto.setIsExpiryTracked(getVal(row, 8, formatter));
        list.add(dto);
      }
    } catch (Exception e) {
      throw new RuntimeException("Excel parsing failed: " + e.getMessage());
    }
    return list;
  }

  private String getVal(Row row, int index, DataFormatter formatter) {
    return formatter.formatCellValue(row.getCell(index)).trim();
  }

  private boolean isRowEmpty(Row row) {
    for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
      if (row.getCell(c) != null && !row.getCell(c).toString().trim().isEmpty()) {
        return false;
      }
    }
    return true;
  }
}
