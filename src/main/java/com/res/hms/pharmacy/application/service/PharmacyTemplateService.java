package com.res.hms.pharmacy.application.service;

import com.res.hms.pharmacy.domain.MedicineForm;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class PharmacyTemplateService {

  public static byte[] generateMedicineTemplate() throws IOException {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Medicine Template");
      CellStyle headerStyle =
          PharmacyExcelHelper.createStyle(
              workbook, IndexedColors.GREY_25_PERCENT, FillPatternType.SOLID_FOREGROUND, true);

      String[] columns = {
        "Name", "Generic Name", "Composition", "Strength", "Form",
        "Manufacturer", "Category", "Threshold", "Batch Tracked", "Expiry Tracked"
      };
      Row header = sheet.createRow(0);
      for (int i = 0; i < columns.length; i++) {
        Cell cell = header.createCell(i);
        cell.setCellValue(columns[i]);
        cell.setCellStyle(headerStyle);
        sheet.autoSizeColumn(i);
      }

      addDummyMedicineRow(sheet.createRow(1));

      DataValidationHelper helper = sheet.getDataValidationHelper();
      String[] forms =
          java.util.Arrays.stream(MedicineForm.values()).map(Enum::name).toArray(String[]::new);
      addValidation(sheet, helper, forms, 1, 1000, 4, 4);
      addValidation(sheet, helper, new String[] {"YES", "NO"}, 1, 1000, 8, 9);

      addInstructionSheet(workbook, getMedicineInstructions());

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      workbook.write(bos);
      return bos.toByteArray();
    }
  }

  public static byte[] generateConsumableTemplate() throws IOException {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Consumable Template");
      CellStyle headerStyle =
          PharmacyExcelHelper.createStyle(
              workbook, IndexedColors.GREY_25_PERCENT, FillPatternType.SOLID_FOREGROUND, true);

      String[] columns = {
        "Department",
        "Category",
        "Item Code",
        "Item Name",
        "Base Unit",
        "Manufacturer",
        "Low Stock Alert",
        "Is Batch Tracked",
        "Is Expiry Tracked"
      };
      Row header = sheet.createRow(0);
      for (int i = 0; i < columns.length; i++) {
        Cell cell = header.createCell(i);
        cell.setCellValue(columns[i]);
        cell.setCellStyle(headerStyle);
        sheet.autoSizeColumn(i);
      }

      addDummyConsumableRow(sheet.createRow(1));

      DataValidationHelper helper = sheet.getDataValidationHelper();
      addValidation(sheet, helper, new String[] {"PHARMACY", "LABORATORY"}, 1, 1000, 0, 0);
      addValidation(sheet, helper, new String[] {"YES", "NO"}, 1, 1000, 7, 8);

      addInstructionSheet(workbook, getConsumableInstructions());

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      workbook.write(bos);
      return bos.toByteArray();
    }
  }

  public static byte[] generateStockTemplate() throws IOException {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Stock Template");
      CellStyle headerStyle =
          PharmacyExcelHelper.createStyle(
              workbook, IndexedColors.GREY_25_PERCENT, FillPatternType.SOLID_FOREGROUND, true);

      String[] columns = {
        "Medicine Name",
        "Batch Number",
        "Expiry Date (YYYY-MM-DD)",
        "Manufacturing Date (YYYY-MM-DD)",
        "Quantity",
        "Purchase Price",
        "Selling Price",
        "HSN Code",
        "Supplier State",
        "Tax Inclusive (Y/N)"
      };
      Row header = sheet.createRow(0);
      for (int i = 0; i < columns.length; i++) {
        Cell cell = header.createCell(i);
        cell.setCellValue(columns[i]);
        cell.setCellStyle(headerStyle);
        sheet.autoSizeColumn(i);
      }

      addDummyStockRow(sheet.createRow(1));

      DataValidationHelper helper = sheet.getDataValidationHelper();
      addValidation(sheet, helper, new String[] {"Y", "N"}, 1, 1000, 9, 9);

      addInstructionSheet(workbook, getStockInstructions());

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      workbook.write(bos);
      return bos.toByteArray();
    }
  }

  private static void addDummyMedicineRow(Row row) {
    row.createCell(0).setCellValue("Paracetamol 500mg");
    row.createCell(1).setCellValue("Acetaminophen");
    row.createCell(2).setCellValue("Paracetamol IP 500mg");
    row.createCell(3).setCellValue("500mg");
    row.createCell(4).setCellValue("TABLET");
    row.createCell(5).setCellValue("Cipla");
    row.createCell(6).setCellValue("Antipyretic");
    row.createCell(7).setCellValue(100);
    row.createCell(8).setCellValue("YES");
    row.createCell(9).setCellValue("YES");
  }

  private static void addDummyConsumableRow(Row row) {
    row.createCell(0).setCellValue("PHARMACY");
    row.createCell(1).setCellValue("Disposable");
    row.createCell(2).setCellValue("CON001");
    row.createCell(3).setCellValue("Syringe 5ml");
    row.createCell(4).setCellValue("PCS");
    row.createCell(5).setCellValue("BD India");
    row.createCell(6).setCellValue(100);
    row.createCell(7).setCellValue("YES");
    row.createCell(8).setCellValue("NO");
  }

  private static void addDummyStockRow(Row row) {
    row.createCell(0).setCellValue("Paracetamol 500mg");
    row.createCell(1).setCellValue("BATCH001");
    row.createCell(2).setCellValue(java.time.LocalDate.now().plusYears(1).toString());
    row.createCell(3).setCellValue(java.time.LocalDate.now().minusMonths(1).toString());
    row.createCell(4).setCellValue(100);
    row.createCell(5).setCellValue(10.50);
    row.createCell(6).setCellValue(15.00);
    row.createCell(7).setCellValue("3004");
    row.createCell(8).setCellValue("Maharashtra");
    row.createCell(9).setCellValue("Y");
  }

  private static void addValidation(
      Sheet sheet, DataValidationHelper helper, String[] opts, int r1, int r2, int c1, int c2) {
    DataValidationConstraint constraint = helper.createExplicitListConstraint(opts);
    CellRangeAddressList addressList = new CellRangeAddressList(r1, r2, c1, c2);
    DataValidation validation = helper.createValidation(constraint, addressList);
    validation.setShowErrorBox(true);
    validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
    validation.createErrorBox("Invalid Value", "Please select a value from the list.");
    sheet.addValidationData(validation);
  }

  private static void addInstructionSheet(Workbook wb, String[][] data) {
    Sheet infoSheet = wb.createSheet("Instructions");
    Row header = infoSheet.createRow(0);
    header.createCell(0).setCellValue("Column");
    header.createCell(1).setCellValue("Requirement");
    header.createCell(2).setCellValue("Valid Values");

    for (int i = 0; i < data.length; i++) {
      Row r = infoSheet.createRow(i + 1);
      r.createCell(0).setCellValue(data[i][0]);
      r.createCell(1).setCellValue(data[i][1]);
      r.createCell(2).setCellValue(data[i][2]);
    }
    infoSheet.autoSizeColumn(0);
    infoSheet.autoSizeColumn(1);
    infoSheet.autoSizeColumn(2);
  }

  private static String[][] getMedicineInstructions() {
    return new String[][] {
      {"Name", "Mandatory", "Text (Brand Name + Strength)"},
      {"Generic Name", "Mandatory", "Text (Chemical Name)"},
      {"Composition", "Mandatory", "Text (Full Salt Details)"},
      {"Strength", "Mandatory", "Text (e.g. 500mg, 5ml)"},
      {"Form", "Mandatory", "Choose from Dropdown (TABLET, SYRUP, etc.)"},
      {"Manufacturer", "Mandatory", "Text (Company Name)"},
      {"Category", "Optional", "Text (e.g. General, Critical)"},
      {"Threshold", "Optional", "Number (Reorder Level)"},
      {"Batch Tracked", "Mandatory", "YES / NO"},
      {"Expiry Tracked", "Mandatory", "YES / NO"}
    };
  }

  private static String[][] getConsumableInstructions() {
    return new String[][] {
      {"Department", "Mandatory", "PHARMACY / LABORATORY"},
      {"Category", "Mandatory", "Text (e.g. Disposable, Glassware)"},
      {"Item Code", "Mandatory", "Unique Alphanumeric Code"},
      {"Item Name", "Mandatory", "Display Name of Item"},
      {"Base Unit", "Mandatory", "PCS, PKT, BOX, etc."},
      {"Manufacturer", "Mandatory", "Brand Name"},
      {"Low Stock Alert", "Optional", "Number (Threshold)"},
      {"Is Batch Tracked", "Mandatory", "YES / NO"},
      {"Is Expiry Tracked", "Mandatory", "YES / NO"}
    };
  }

  private static String[][] getStockInstructions() {
    return new String[][] {
      {"Medicine Name", "Mandatory", "Must exist in Medicine Master"},
      {"Batch Number", "Mandatory", "Unique Batch Identifier"},
      {"Expiry Date", "Mandatory", "Format: YYYY-MM-DD"},
      {"Manufacturing Date", "Optional", "Format: YYYY-MM-DD"},
      {"Quantity", "Mandatory", "Positive Number"},
      {"Purchase Price", "Mandatory", "Positive Number"},
      {"Selling Price", "Mandatory", "Positive Number (>= Purchase Price)"},
      {"HSN Code", "Optional", "Text"},
      {"Supplier State", "Optional", "Text"},
      {"Tax Inclusive", "Mandatory", "Y / N"}
    };
  }
}
