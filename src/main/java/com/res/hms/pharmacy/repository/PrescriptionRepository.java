package com.res.hms.pharmacy.repository;

import com.res.hms.domain.Prescription;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

  Optional<Prescription> findTopByPatient_IdAndDoctor_IdOrderByCreatedAtDesc(
      Long patientId, Long doctorId);

  Optional<Prescription> findByIdAndHospitalId(Long id, Long hospitalId);
}
