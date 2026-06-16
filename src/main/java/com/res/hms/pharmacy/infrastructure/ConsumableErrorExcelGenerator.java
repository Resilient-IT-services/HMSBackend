package com.res.hms.pharmacy.infrastructure;

import com.res.hms.pharmacy.application.dto.ConsumableImportDto;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class ConsumableErrorExcelGenerator {

  public String generate(List<ConsumableImportDto> failedRows) {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Import_Errors");
      Row header = sheet.createRow(0);
      String[] columns = {"Row Number", "Item Code", "Item Name", "Error Log"};
      for (int i = 0; i < columns.length; i++) {
        header.createCell(i).setCellValue(columns[i]);
      }

      int rowIdx = 1;
      for (ConsumableImportDto dto : failedRows) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(dto.getRowNumber());
        row.createCell(1).setCellValue(dto.getItemCode());
        row.createCell(2).setCellValue(dto.getItemName());
        row.createCell(3).setCellValue(dto.getErrorLog());
      }
      workbook.write(bos);
      return Base64.getEncoder().encodeToString(bos.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException("Error file generation failed: " + e.getMessage());
    }
  }
}
