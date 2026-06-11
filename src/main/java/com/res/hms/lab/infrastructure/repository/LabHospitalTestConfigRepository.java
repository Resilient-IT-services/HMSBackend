package com.res.hms.lab.infrastructure.repository;

import com.res.hms.lab.domain.LabHospitalTestConfig;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabHospitalTestConfigRepository
    extends JpaRepository<LabHospitalTestConfig, UUID> {
  Optional<LabHospitalTestConfig> findByHospitalIdAndTestId(UUID hospitalId, UUID testId);

  long countByHospitalIdAndActiveTrue(UUID hospitalId);
}
