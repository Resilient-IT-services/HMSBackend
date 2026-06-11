package com.res.hms.lab.infrastructure.repository;

import com.res.hms.lab.domain.LabPrice;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LabPriceRepository
    extends JpaRepository<LabPrice, UUID>, JpaSpecificationExecutor<LabPrice> {
  java.util.Optional<LabPrice> findByHospitalIdAndTestId(UUID hospitalId, UUID testId);
}
