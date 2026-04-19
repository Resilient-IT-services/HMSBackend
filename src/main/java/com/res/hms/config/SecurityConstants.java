package com.res.hms.config;

public final class SecurityConstants {
  public static final String HAS_ADMIN_OR_PHARMACIST = "hasAnyRole('ADMIN','PHARMACIST')";
  public static final String HAS_ADMIN_OR_DOCTOR = "hasAnyRole('ADMIN','DOCTOR')";
  public static final String HAS_ADMIN_PHARMACIST_OR_DOCTOR =
      "hasAnyRole('ADMIN','PHARMACIST','DOCTOR')";

  private SecurityConstants() {
    // Utility class
  }
}
