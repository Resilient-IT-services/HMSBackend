package com.res.hms.core.tenant;

import java.util.UUID;

public class TenantContext {
  private static final ThreadLocal<UUID> CURRENT_HOSPITAL_ID = new ThreadLocal<>();

  public static void setHospitalId(UUID hospitalId) {
    CURRENT_HOSPITAL_ID.set(hospitalId);
  }

  public static UUID getHospitalId() {
    return CURRENT_HOSPITAL_ID.get();
  }

  public static void clear() {
    CURRENT_HOSPITAL_ID.remove();
  }
}
