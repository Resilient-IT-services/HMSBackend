package com.res.hms.pharmacy.repository;

import com.res.hms.domain.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    Page<Medicine> findAllByHospitalIdOrderByCreatedAtDesc(Long hospitalId, Pageable pageable);
    Optional<Medicine> findByIdAndHospitalId(Long id, Long hospitalId);
}
