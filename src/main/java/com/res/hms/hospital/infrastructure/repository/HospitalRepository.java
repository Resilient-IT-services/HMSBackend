package com.res.hms.hospital.infrastructure.repository;

import com.res.hms.hospital.domain.Hospital;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HospitalRepository
    extends JpaRepository<Hospital, UUID>, JpaSpecificationExecutor<Hospital> {
  Optional<Hospital> findByCode(String code);

  boolean existsByNameIgnoreCase(String name);

  boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

  boolean existsByCode(String code);

  boolean existsByCodeAndIdNot(String code, UUID id);

  boolean existsByGstin(String gstin);

  boolean existsByGstinAndIdNot(String gstin, UUID id);

  long countByDeleted(boolean deleted);

  long countByCreatedAtAfter(java.time.Instant date);
}
