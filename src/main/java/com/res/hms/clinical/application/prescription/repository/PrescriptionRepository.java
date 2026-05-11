package com.res.hms.clinical.application.prescription.repository;

import com.res.hms.clinical.domain.Prescription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

  @Query("SELECT DISTINCT p FROM Prescription p LEFT JOIN FETCH p.patient LEFT JOIN FETCH p.items WHERE p.hospitalId = :hospitalId ORDER BY p.createdAt DESC")
  Page<Prescription> findAllByHospitalId(UUID hospitalId, Pageable pageable);

  @Query(
      """
      SELECT DISTINCT p FROM Prescription p 
      LEFT JOIN FETCH p.patient 
      LEFT JOIN FETCH p.items 
      WHERE p.hospitalId = :hospitalId
        AND (LOWER(p.patientName) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(p.doctorName) LIKE LOWER(CONCAT('%', :search, '%'))
             OR CAST(p.id AS string) LIKE LOWER(CONCAT('%', :search, '%')))
      ORDER BY p.createdAt DESC
      """)
  Page<Prescription> searchPrescriptions(
      UUID hospitalId, String search, Pageable pageable);

  @Query(
      "SELECT p FROM Prescription p LEFT JOIN FETCH p.patient WHERE p.patientId = :patientId AND p.hospitalId = :hospitalId")
  List<Prescription> findByPatientIdAndHospitalId(UUID patientId, UUID hospitalId);

  @Query("SELECT p FROM Prescription p LEFT JOIN FETCH p.patient WHERE p.id = :id AND p.hospitalId = :hospitalId")
  Optional<Prescription> findByIdAndHospitalId(UUID id, UUID hospitalId);

  Optional<Prescription> findByAppointmentId(UUID appointmentId);

  boolean existsByAppointmentId(UUID appointmentId);
}
