package com.res.hms.clinical.infrastructure.repository;

import com.res.hms.clinical.domain.Patient;
import com.res.hms.core.repository.BaseRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository
    extends BaseRepository<Patient>, JpaSpecificationExecutor<Patient> {
  boolean existsByPatientCode(String patientCode);

  boolean existsByPatientCodeAndIdNot(String patientCode, UUID id);

  boolean existsByFullNameAndPhoneAndHospitalId(String fullName, String phone, UUID hospitalId);

  boolean existsByFullNameAndPhoneAndHospitalIdAndIdNot(
      String fullName, String phone, UUID hospitalId, UUID id);

  long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

  List<Patient> findAllByPatientCodeIsNullOrderByCreatedAtAsc();

  @Query(
      value =
          "SELECT p.patient_code FROM patient.patient  p WHERE p.hospital_id = :hospitalId AND p.patient_code LIKE :prefix ORDER BY p.patient_code DESC LIMIT 1",
      nativeQuery = true)
  Optional<String> findLastPatientCode(
      @Param("hospitalId") UUID hospitalId, @Param("prefix") String prefix);

  @Query(
      value =
          """
        SELECT p.patient_code
        FROM patient.patient p
        WHERE p.patient_code LIKE :prefix
        ORDER BY p.patient_code DESC
        LIMIT 1
    """,
      nativeQuery = true)
  Optional<String> findLastPatientCodeGlobal(@Param("prefix") String prefix);

  @Query(
      "SELECT p.hospitalId, COUNT(p.id) as patientCount FROM Patient p GROUP BY p.hospitalId ORDER BY patientCount DESC")
  List<Object[]> findTopHospitalsByPatientCount(org.springframework.data.domain.Pageable pageable);
}
