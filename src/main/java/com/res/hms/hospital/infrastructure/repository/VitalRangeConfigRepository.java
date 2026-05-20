package com.res.hms.hospital.infrastructure.repository;

import com.res.hms.hospital.domain.VitalRangeConfig;
import com.res.hms.hospital.domain.VitalType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VitalRangeConfigRepository extends JpaRepository<VitalRangeConfig, UUID> {
  Optional<VitalRangeConfig> findByVitalType(VitalType vitalType);
}
