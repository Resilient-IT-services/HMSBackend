package com.res.hms.patient.repository;

import com.res.hms.common.repository.BaseRepository;
import com.res.hms.domain.Patient;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends BaseRepository<Patient> {
  @Query(
      "SELECT p FROM Patient p WHERE p.hospitalId = :hospitalId AND "
          + "(LOWER(p.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR "
          + "p.phone LIKE CONCAT('%', :query, '%'))")
  Page<Patient> search(
      @Param("hospitalId") UUID hospitalId, @Param("query") String query, Pageable pageable);
}
