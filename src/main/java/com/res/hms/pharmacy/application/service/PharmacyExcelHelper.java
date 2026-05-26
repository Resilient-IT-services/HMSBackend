package com.res.hms.pharmacy.application.service;

import com.res.hms.pharmacy.domain.MedicineForm;
import com.res.hms.pharmacy.domain.MedicineSchedule;
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
      String name,
      String genericName,
      String composition,
      String strength,
      String formStr,
      String manufacturer) {
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

    return errors;
  }

  public static List<String> validateStockData(
      String medicineName,
      String batchNumber,
      String qtyStr,
      String sellingPriceStr,
      String purchasePriceStr,
      String expiryStr) {
    List<String> errors = new ArrayList<>();

    if (!hasText(medicineName)) {
      errors.add("Medicine Name is missing");
    }

    if (!hasText(batchNumber)) {
      errors.add("Batch Number is missing");
    }

    if (!hasText(qtyStr)) {
      errors.add("Quantity is missing");
    } else {
      try {
        double q = Double.parseDouble(qtyStr);
        if (q < 0) {
          errors.add("Quantity cannot be negative");
        }
      } catch (NumberFormatException e) {
        errors.add("Invalid Quantity format: " + qtyStr);
      }
    }

    if (!hasText(sellingPriceStr)) {
      errors.add("Selling Price is missing");
    } else {
      try {
        new java.math.BigDecimal(sellingPriceStr);
      } catch (Exception e) {
        errors.add("Invalid Selling Price format: " + sellingPriceStr);
      }
    }

    if (hasText(purchasePriceStr)) {
      try {
        new java.math.BigDecimal(purchasePriceStr);
      } catch (Exception e) {
        errors.add("Invalid Purchase Price format: " + purchasePriceStr);
      }
    }

    return errors;
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

  public static MedicineSchedule parseMedicineSchedule(String val) {
    if (!hasText(val)) {
      return MedicineSchedule.OTC;
    }
    String s = val.trim().toUpperCase();
    try {
      MedicineSchedule mapped = mapScheduleFallback(s);
      return mapped != null ? mapped : MedicineSchedule.valueOf(s);
    } catch (Exception e) {
      return null;
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

  public static MedicineSchedule mapScheduleFallback(String s) {
    if (s.equals("H") || s.contains("SCH_H") || s.contains("SCHEDULE_H")) {
      return MedicineSchedule.SCHEDULE_H;
    }
    if (s.equals("G") || s.contains("SCH_G") || s.contains("SCHEDULE_G")) {
      return MedicineSchedule.SCHEDULE_G;
    }
    if (s.equals("X") || s.contains("SCH_X") || s.contains("SCHEDULE_X")) {
      return MedicineSchedule.SCHEDULE_X;
    }
    if (s.equals("OTC")) {
      return MedicineSchedule.OTC;
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

  public static Cell getOrCreateCell(Row r, int c) {
    Cell cell = r.getCell(c);
    return cell != null ? cell : r.createCell(c);
  }

  public static String text(Row r, int c) {
    Cell v = r.getCell(c);
    return v == null ? null : DATA_FORMATTER.formatCellValue(v).trim();
  }

  public static boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }

  public static boolean isBlank(Row r, int c) {
    return !hasText(text(r, c));
  }
}
