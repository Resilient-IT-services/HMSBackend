package com.res.hms.pharmacy.application.service;

import com.res.hms.pharmacy.domain.MedicineForm;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class PharmacyExcelHelper {

  private static final DataFormatter DATA_FORMATTER = new DataFormatter();

  public static List<String> validateMedicineData(
      String name, String gName, String comp, String str, String fStr, String mf) {
    List<String> errors = new ArrayList<>();
    if (!hasText(name)) {
      errors.add("Name is required");
    }
    if (!hasText(gName)) {
      errors.add("Generic Name is required");
    }
    if (!hasText(comp)) {
      errors.add("Salt/Composition is required");
    }
    if (!hasText(str)) {
      errors.add("Strength is required");
    }
    if (!hasText(mf)) {
      errors.add("Manufacturer is required");
    }

    MedicineForm form = parseMedicineForm(fStr);
    if (form == null) {
      String accepted = java.util.Arrays.toString(MedicineForm.values());
      errors.add("Medicine Form is required. Accepted: " + accepted);
    }
    return errors;
  }

  public static List<String> validateStockData(
      String name, String batch, String qty, String sp, String pp, String exp) {
    List<String> errors = new ArrayList<>();
    validateStockRequiredFields(name, batch, qty, pp, sp, exp, errors);
    validateStockNumericFields(qty, pp, sp, errors);
    return errors;
  }

  private static void validateStockRequiredFields(
      String n, String b, String q, String p, String s, String e, List<String> errors) {
    if (!hasText(n)) {
      errors.add("[Medicine Name] is missing");
    }
    if (!hasText(b)) {
      errors.add("[Batch Number] is missing");
    }
    if (!hasText(e)) {
      errors.add("[Expiry Date] is missing");
    }
    if (!hasText(q)) {
      errors.add("[Quantity] is missing");
    }
    if (!hasText(p)) {
      errors.add("[Purchase Price] is missing");
    }
    if (!hasText(s)) {
      errors.add("[Selling Price] is missing");
    }
  }

  private static void validateStockNumericFields(
      String q, String p, String s, List<String> errors) {
    if (hasText(q) && q.replaceAll("[^\\d.]", "").isEmpty()) {
      errors.add("[Quantity] Invalid: " + q);
    }
    if (hasText(p) && parseBigDecimal(p).compareTo(java.math.BigDecimal.ZERO) <= 0) {
      errors.add("[Purchase Price] must be greater than zero");
    }
    if (hasText(s) && parseBigDecimal(s).compareTo(java.math.BigDecimal.ZERO) <= 0) {
      errors.add("[Selling Price] must be greater than zero");
    }
    if (hasText(p) && hasText(s)) {
      if (parseBigDecimal(s).compareTo(parseBigDecimal(p)) < 0) {
        errors.add("selling price is less than purchase price");
      }
    }
  }

  public static boolean isRowEmpty(Row row) {
    if (row == null) {
      return true;
    }
    for (int i = 0; i < row.getLastCellNum(); i++) {
      if (hasText(text(row, i))) {
        return false;
      }
    }
    return true;
  }

  public static MedicineForm parseMedicineForm(String val) {
    if (!hasText(val)) {
      return null;
    }
    String s = val.trim().toUpperCase();
    try {
      return MedicineForm.valueOf(s);
    } catch (Exception e) {
      return mapFormFallback(s);
    }
  }

  public static MedicineForm mapFormFallback(String s) {
    if (s.contains("TAB")) {
      return MedicineForm.TABLET;
    }
    if (s.contains("CAP")) {
      return MedicineForm.CAPSULE;
    }
    if (s.contains("INJ")) {
      return MedicineForm.INJECTION;
    }
    if (s.contains("SYR")) {
      return MedicineForm.SYRUP;
    }
    if (s.contains("DRP") || s.contains("DROP")) {
      return MedicineForm.DROPS;
    }
    if (s.contains("CREAM")) {
      return MedicineForm.CREAM;
    }
    if (s.contains("OINT")) {
      return MedicineForm.OINTMENT;
    }
    if (s.contains("INH")) {
      return MedicineForm.INHALER;
    }
    return null;
  }

  public static CellStyle[] createFeedbackStyles(Workbook wb) {
    return new CellStyle[] {
      createStyle(wb, IndexedColors.GREY_25_PERCENT, FillPatternType.SOLID_FOREGROUND, true),
      createStyle(wb, IndexedColors.LIGHT_GREEN, FillPatternType.SOLID_FOREGROUND, false),
      createStyle(wb, IndexedColors.LIGHT_YELLOW, FillPatternType.SOLID_FOREGROUND, false),
      createStyle(wb, IndexedColors.RED, FillPatternType.SOLID_FOREGROUND, false)
    };
  }

  public static CellStyle createStyle(Workbook wb, IndexedColors c, FillPatternType p, boolean b) {
    CellStyle s = wb.createCellStyle();
    s.setFillForegroundColor(c.getIndex());
    s.setFillPattern(p);
    if (b) {
      var f = wb.createFont();
      f.setBold(true);
      s.setFont(f);
    }
    return s;
  }

  public static int prepareHeaderAndGetStatusColumn(Sheet s, CellStyle h) {
    Row r = s.getRow(0);
    if (r == null) {
      r = s.createRow(0);
    }
    for (int i = 0; i < r.getLastCellNum(); i++) {
      if ("IMPORT STATUS".equalsIgnoreCase(text(r, i))) {
        return i;
      }
    }
    int idx = r.getLastCellNum();
    Cell c = r.createCell(idx);
    c.setCellValue("IMPORT STATUS");
    c.setCellStyle(h);
    return idx;
  }

  public static String getFeedbackFileBase64(Workbook wb) {
    try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
      wb.write(b);
      return Base64.getEncoder().encodeToString(b.toByteArray());
    } catch (IOException e) {
      return null;
    }
  }

  public static void updateCell(Cell c, String v, CellStyle s) {
    c.setCellValue(v);
    c.setCellStyle(s);
  }

  public static Cell findHeaderCell(Sheet s, String name) {
    Row r = s.getRow(0);
    if (r == null) {
      return null;
    }
    for (int i = 0; i < r.getLastCellNum(); i++) {
      if (name.equalsIgnoreCase(text(r, i))) {
        return r.getCell(i);
      }
    }
    return null;
  }

  public static Cell getOrCreateCell(Row r, int c) {
    Cell cell = r.getCell(c);
    return cell == null ? r.createCell(c) : cell;
  }

  public static String text(Row r, int c) {
    Cell cell = r.getCell(c);
    if (cell == null) {
      return null;
    }
    try {
      if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
        return cell.getStringCellValue().trim();
      }
      if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
        if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
          return cell.getLocalDateTimeCellValue().toLocalDate().toString();
        }
        return DATA_FORMATTER.formatCellValue(cell).trim();
      }
      return DATA_FORMATTER.formatCellValue(cell).trim();
    } catch (Exception e) {
      return "";
    }
  }

  public static boolean hasText(String v) {
    return v != null && !v.trim().isEmpty();
  }

  public static java.time.LocalDate parseDate(String val) {
    if (!hasText(val)) {
      return null;
    }
    String s = val.trim().replaceAll("[/. ]", "-");
    String[] patterns = {
      "yyyy-MM-dd", "dd-MM-yyyy", "dd-MMM-yyyy", "MM-yyyy", "MMM-yyyy", "dd-MM-yy", "MMM-yy"
    };
    for (String p : patterns) {
      try {
        if ((p.endsWith("-yy") || p.endsWith("-yyyy")) && !p.contains("dd")) {
          return java.time.LocalDate.parse(
              "01-" + s,
              java.time.format.DateTimeFormatter.ofPattern("dd-" + p, java.util.Locale.ENGLISH));
        }
        return java.time.LocalDate.parse(
            s, java.time.format.DateTimeFormatter.ofPattern(p, java.util.Locale.ENGLISH));
      } catch (Exception e) {
        // ignore
      }
    }
    return parseDateFallback(s);
  }

  private static java.time.LocalDate parseDateFallback(String s) {
    String d = s.replaceAll("[^\\d]", "");
    try {
      if (d.length() == 6) {
        return java.time.LocalDate.parse(d, java.time.format.DateTimeFormatter.ofPattern("ddMMyy"));
      }
      if (d.length() == 8) {
        return java.time.LocalDate.parse(
            d, java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy"));
      }
    } catch (Exception e) {
      // ignore
    }
    return null;
  }

  public static Integer parseInteger(String s) {
    try {
      if (hasText(s)) {
        return (int) Double.parseDouble(s.replaceAll("[^\\d.]", ""));
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  public static java.math.BigDecimal parseBigDecimal(String s) {
    try {
      String stripped = hasText(s) ? s.replaceAll("[^\\d.]", "") : "";
      return stripped.isEmpty() ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(stripped);
    } catch (Exception e) {
      return java.math.BigDecimal.ZERO;
    }
  }

  public static int parseQuantity(String q) {
    try {
      String stripped = hasText(q) ? q.replaceAll("[^\\d.]", "") : "";
      return stripped.isEmpty() ? 0 : (int) Double.parseDouble(stripped);
    } catch (Exception e) {
      return 0;
    }
  }
}
