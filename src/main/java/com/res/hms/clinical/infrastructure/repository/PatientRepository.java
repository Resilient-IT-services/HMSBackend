package com.res.hms.clinical.infrastructure.repository;

import com.res.hms.clinical.domain.Patient;
import com.res.hms.core.repository.BaseRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends BaseRepository<Patient> {
  boolean existsByPatientCode(String patientCode);

  boolean existsByPatientCodeAndIdNot(String patientCode, UUID id);

  @Query(
      "SELECT COUNT(p) > 0 FROM Patient p WHERE p.hospitalId = :hospitalId AND "
          + "LOWER(TRIM(p.fullName)) = LOWER(TRIM(:fullName)) AND "
          + "TRIM(p.phone) = TRIM(:phone)")
  boolean existsByFullNameAndPhoneAndHospitalId(
      @Param("fullName") String fullName,
      @Param("phone") String phone,
      @Param("hospitalId") UUID hospitalId);

  @Query(
      "SELECT COUNT(p) > 0 FROM Patient p WHERE p.hospitalId = :hospitalId AND "
          + "LOWER(TRIM(p.fullName)) = LOWER(TRIM(:fullName)) AND "
          + "TRIM(p.phone) = TRIM(:phone) AND p.id != :id")
  boolean existsByFullNameAndPhoneAndHospitalIdAndIdNot(
      @Param("fullName") String fullName,
      @Param("phone") String phone,
      @Param("hospitalId") UUID hospitalId,
      @Param("id") UUID id);

  List<Patient> findAllByPatientCodeIsNullOrderByCreatedAtAsc();

  @Query(
      value =
          "SELECT p.patient_code FROM patient p WHERE p.hospital_id = :hospitalId AND p.patient_code LIKE :prefix ORDER BY p.patient_code DESC LIMIT 1",
      nativeQuery = true)
  Optional<String> findLastPatientCode(
      @Param("hospitalId") UUID hospitalId, @Param("prefix") String prefix);

  @Query(
      value =
          "SELECT p.patient_code FROM patient p WHERE p.patient_code LIKE :prefix ORDER BY LENGTH(p.patient_code) DESC, p.patient_code DESC LIMIT 1",
      nativeQuery = true)
  Optional<String> findLastPatientCodeGlobal(@Param("prefix") String prefix);

  @Query(
      "SELECT p FROM Patient p WHERE p.hospitalId = :hospitalId AND "
          + "(LOWER(p.fullName) LIKE :query OR "
          + "LOWER(p.patientCode) LIKE :query OR "
          + "p.phone LIKE :query)")
  Page<Patient> search(
      @Param("hospitalId") UUID hospitalId, @Param("query") String query, Pageable pageable);
}
