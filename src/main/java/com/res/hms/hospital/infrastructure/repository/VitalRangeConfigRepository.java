package com.res.hms.hospital.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.hospital.domain.VitalRangeConfig;
import com.res.hms.hospital.domain.VitalType;

@Repository
public interface VitalRangeConfigRepository extends BaseRepository<VitalRangeConfig> {
  List<VitalRangeConfig> findAllByHospitalId(UUID hospitalId);

  Optional<VitalRangeConfig> findByHospitalIdAndVitalType(UUID hospitalId, VitalType vitalType);
}
