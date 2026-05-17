package com.res.hms.hospital.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.res.hms.hospital.domain.VitalRangeConfig;
import com.res.hms.hospital.domain.VitalType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface VitalRangeConfigRepository extends JpaRepository<VitalRangeConfig, UUID> {
  Optional<VitalRangeConfig> findByVitalType(VitalType vitalType);
}
