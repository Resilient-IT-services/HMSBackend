package com.res.hms.config;

public final class SecurityConstants {
  public static final String HAS_ADMIN_OR_PHARMACIST =
      "hasAnyRole('ADMIN','PHARMACIST','SUPER_ADMIN')";
  public static final String HAS_ADMIN_OR_DOCTOR = "hasAnyRole('ADMIN','DOCTOR','SUPER_ADMIN')";
  public static final String HAS_ADMIN_PHARMACIST_OR_DOCTOR =
      "hasAnyRole('ADMIN','PHARMACIST','DOCTOR','SUPER_ADMIN')";

  private SecurityConstants() {
    // Utility class
  }
}
