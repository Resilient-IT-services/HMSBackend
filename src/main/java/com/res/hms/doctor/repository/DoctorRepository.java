package com.res.hms.doctor.repository;

import com.res.hms.common.repository.BaseRepository;
import com.res.hms.domain.Doctor;

import java.util.Optional;

public interface DoctorRepository extends BaseRepository<Doctor, Long> {
  Optional<Doctor> findByUserIdAndHospitalId(Long userId, Long hospitalId);
}
