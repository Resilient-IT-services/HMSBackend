package com.res.hms.pharmacy.application.service;

import com.res.hms.pharmacy.domain.MedicineForm;
import com.res.hms.pharmacy.domain.MedicineSchedule;
import java.util.ArrayList;
import java.util.List;

public class PharmacyExcelHelper {

  public static List<String> validateMedicineData(String name, String genericName, String composition, String strength, String formStr, String manufacturer) {
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

  public static List<String> validateStockData(String medicineName, String batchNumber, String qtyStr, String sellingPriceStr, String purchasePriceStr, String expiryStr) {
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

  private static boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
