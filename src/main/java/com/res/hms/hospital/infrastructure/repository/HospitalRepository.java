package com.res.hms.hospital.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.res.hms.hospital.domain.Hospital;

public interface HospitalRepository
    extends JpaRepository<Hospital, UUID>, JpaSpecificationExecutor<Hospital> {
  Optional<Hospital> findByCode(String code);

  boolean existsByCode(String code);

  boolean existsByCodeAndIdNot(String code, UUID id);

  boolean existsByGstin(String gstin);

  boolean existsByGstinAndIdNot(String gstin, UUID id);
}
