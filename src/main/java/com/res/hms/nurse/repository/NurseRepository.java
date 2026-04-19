package com.res.hms.nurse.repository;

import com.res.hms.domain.Nurse;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NurseRepository extends JpaRepository<Nurse, Long> {
  Page<Nurse> findAllByHospitalIdOrderByCreatedAtDesc(Long hospitalId, Pageable pageable);

  Optional<Nurse> findByIdAndHospitalId(Long id, Long hospitalId);
}
