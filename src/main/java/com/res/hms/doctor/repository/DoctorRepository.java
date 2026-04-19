package com.res.hms.doctor.repository;

import com.res.hms.domain.Doctor;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
  Page<Doctor> findAllByHospitalIdOrderByCreatedAtDesc(Long hospitalId, Pageable pageable);

  Optional<Doctor> findByIdAndHospitalId(Long id, Long hospitalId);
}
