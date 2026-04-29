package com.res.hms.repository;

import com.res.hms.common.repository.BaseRepository;
import com.res.hms.domain.VitalSetting;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VitalSettingRepository extends BaseRepository<VitalSetting> {
  List<VitalSetting> findAllByHospitalId(UUID hospitalId);

  Optional<VitalSetting> findByHospitalIdAndVitalName(UUID hospitalId, String vitalName);
}
