package com.res.hms.pharmacy.repository;

import com.res.hms.domain.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    Optional<Prescription> findByIdAndHospitalId(Long id, Long hospitalId);
}
