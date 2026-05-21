package com.res.hms.pharmacy.application.service;

import com.res.hms.pharmacy.domain.MedicineForm;
import com.res.hms.pharmacy.domain.MedicineSchedule;

public class PharmacyExcelHelper {

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
}
