package com.res.hms.pharmacy.repository;

import com.res.hms.domain.Prescription;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

  Optional<Prescription> findTopByPatient_IdAndDoctor_IdOrderByCreatedAtDesc(
      UUID patientId, UUID doctorId);

  Optional<Prescription> findByIdAndHospitalId(UUID id, UUID hospitalId);
}
