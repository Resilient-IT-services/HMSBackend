package com.res.hms.clinical.application.patient.repository;

import com.res.hms.clinical.domain.Patient;
import com.res.hms.core.repository.BaseRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends BaseRepository<Patient> {
  boolean existsByPatientCode(String patientCode);

  boolean existsByPatientCodeAndIdNot(String patientCode, UUID id);

  @Query(
      "SELECT p FROM Patient p WHERE p.hospitalId = :hospitalId AND "
          + "(LOWER(p.fullName) LIKE :query OR "
          + "p.phone LIKE :query)")
  Page<Patient> search(
      @Param("hospitalId") UUID hospitalId, @Param("query") String query, Pageable pageable);
}
