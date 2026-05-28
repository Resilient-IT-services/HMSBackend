package com.res.hms.iam.infrastructure.security;

public final class SecurityConstants {
  public static final String HAS_ADMIN = "hasAnyRole('ADMIN','SUPER_ADMIN')";
  public static final String HAS_ADMIN_OR_RECEPTIONIST =
      "hasAnyRole('ADMIN','RECEPTIONIST','SUPER_ADMIN')";
  public static final String HAS_ADMIN_OR_PHARMACIST =
      "hasAnyRole('ADMIN','PHARMACIST','SUPER_ADMIN')";
  public static final String HAS_ADMIN_OR_DOCTOR = "hasAnyRole('ADMIN','DOCTOR','SUPER_ADMIN')";

  public static final String HAS_CLINICAL_STAFF = "hasAnyRole('ADMIN','DOCTOR','NURSE','SUPER_ADMIN')";
  public static final String HAS_PHARMACY_ACCESS =
      "hasAnyRole('ADMIN','PHARMACIST','DOCTOR','NURSE','SUPER_ADMIN')";
  public static final String HAS_RECEPTION_ACCESS =
      "hasAnyRole('ADMIN','RECEPTIONIST','NURSE','DOCTOR','SUPER_ADMIN')";
  public static final String HAS_ANY_STAFF =
      "hasAnyRole('ADMIN','RECEPTIONIST','NURSE','DOCTOR','PHARMACIST','SUPER_ADMIN')";

  public static final String HAS_ADMIN_PHARMACIST_OR_DOCTOR =
      "hasAnyRole('ADMIN','PHARMACIST','DOCTOR','SUPER_ADMIN')";
  public static final String HAS_ADMIN_RECEPTIONIST_OR_DOCTOR =
      "hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR','SUPER_ADMIN')";
  public static final String HAS_ADMIN_DOCTOR_OR_NURSE =
      "hasAnyRole('ADMIN','DOCTOR','NURSE','SUPER_ADMIN')";
  public static final String HAS_ADMIN_RECEPTIONIST_OR_NURSE =
      "hasAnyRole('ADMIN','RECEPTIONIST','NURSE','SUPER_ADMIN')";
  public static final String HAS_ADMIN_RECEPTIONIST_DOCTOR_OR_NURSE =
      "hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR','NURSE','SUPER_ADMIN')";

  private SecurityConstants() {
    // Utility class
  }
}
