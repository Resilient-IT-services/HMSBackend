package com.res.hms.prescription.repository;

import com.res.hms.domain.Prescription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

  @Query(
      "SELECT p FROM Prescription p WHERE p.appointment.hospitalId = :hospitalId ORDER BY p.appointment.createdAt DESC")
  Page<Prescription> findAllByHospitalId(UUID hospitalId, Pageable pageable);

  @Query(
      "SELECT p FROM Prescription p WHERE p.patient.id = :patientId AND p.appointment.hospitalId = :hospitalId")
  List<Prescription> findByPatientIdAndHospitalId(UUID patientId, UUID hospitalId);

  @Query("SELECT p FROM Prescription p WHERE p.id = :id AND p.appointment.hospitalId = :hospitalId")
  Optional<Prescription> findByIdAndHospitalId(UUID id, UUID hospitalId);
}
