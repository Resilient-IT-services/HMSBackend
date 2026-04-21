package com.res.hms.nurse.repository;

import com.res.hms.common.repository.BaseRepository;
import com.res.hms.domain.Nurse;

import java.util.Optional;

public interface NurseRepository extends BaseRepository<Nurse, Long> {
  Optional<Nurse> findByUserIdAndHospitalId(Long userId, Long hospitalId);
}
