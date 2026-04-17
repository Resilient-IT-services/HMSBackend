package com.res.hms.patient.repository;

import com.res.hms.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Page<Patient> findAllByHospitalIdOrderByCreatedAtDesc(Long hospitalId, Pageable pageable);
    Optional<Patient> findByIdAndHospitalId(Long id, Long hospitalId);
}
