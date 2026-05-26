package com.res.hms.clinical.application.prescription.repository;

import com.res.hms.clinical.domain.Prescription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface PrescriptionRepository
    extends JpaRepository<Prescription, UUID>, JpaSpecificationExecutor<Prescription> {

  @Query(
      "SELECT p FROM Prescription p LEFT JOIN FETCH p.patient WHERE p.patientId = :patientId AND p.hospitalId = :hospitalId")
  List<Prescription> findByPatientIdAndHospitalId(UUID patientId, UUID hospitalId);

  @Query(
      "SELECT p FROM Prescription p LEFT JOIN FETCH p.patient WHERE p.id = :id AND p.hospitalId = :hospitalId")
  Optional<Prescription> findByIdAndHospitalId(UUID id, UUID hospitalId);

  Optional<Prescription> findByAppointmentId(UUID appointmentId);

  boolean existsByAppointmentId(UUID appointmentId);
}
