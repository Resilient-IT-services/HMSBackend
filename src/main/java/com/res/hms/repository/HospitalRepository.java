package com.res.hms.repository;

import com.res.hms.domain.Hospital;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HospitalRepository
    extends JpaRepository<Hospital, UUID>, JpaSpecificationExecutor<Hospital> {
  Optional<Hospital> findByCode(String code);
}
