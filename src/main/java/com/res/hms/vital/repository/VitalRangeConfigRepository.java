package com.res.hms.vital.repository;

import com.res.hms.common.repository.BaseRepository;
import com.res.hms.domain.VitalRangeConfig;
import com.res.hms.domain.VitalType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface VitalRangeConfigRepository extends BaseRepository<VitalRangeConfig> {
  List<VitalRangeConfig> findAllByHospitalId(UUID hospitalId);

  Optional<VitalRangeConfig> findByHospitalIdAndVitalType(UUID hospitalId, VitalType vitalType);
}
