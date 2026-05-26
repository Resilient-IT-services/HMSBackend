package com.res.hms.clinical.infrastructure.repository;

import com.res.hms.clinical.domain.Appointment;
import com.res.hms.core.repository.BaseRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository
    extends BaseRepository<Appointment>, JpaSpecificationExecutor<Appointment> {
  @Query(
      "SELECT COUNT(a) FROM Appointment a "
          + "WHERE a.hospitalId = :hospitalId AND "
          + "CAST(a.appointmentTime AS date) > :date")
  long countUpcoming(@Param("hospitalId") UUID hospitalId, @Param("date") java.time.LocalDate date);

  @Query(
      "SELECT a FROM Appointment a "
          + "WHERE a.doctorId = :doctorId "
          + "AND CAST(a.appointmentTime AS date) = :date "
          + "AND a.status NOT IN (com.res.hms.clinical.domain.AppointmentStatus.CANCELLED)")
  java.util.List<Appointment> findByDoctorIdAndDate(
      @Param("doctorId") UUID doctorId, @Param("date") java.time.LocalDate date);

  @Query(
      "SELECT MAX(a.tokenNumber) FROM Appointment a "
          + "WHERE a.doctorId = :doctorId "
          + "AND CAST(a.appointmentTime AS date) = :date")
  Integer findMaxTokenByDoctorAndDate(
      @Param("doctorId") UUID doctorId, @Param("date") java.time.LocalDate date);

  @Query(
      "SELECT COUNT(a) > 0 FROM Appointment a "
          + "WHERE a.patientId = :patientId AND a.doctorId = :doctorId "
          + "AND CAST(a.appointmentTime AS date) = :date "
          + "AND a.status <> :status")
  boolean existsByPatientIdAndDoctorIdAndDateAndStatusNot(
      @Param("patientId") UUID patientId,
      @Param("doctorId") UUID doctorId,
      @Param("date") java.time.LocalDate date,
      @Param("status") com.res.hms.clinical.domain.AppointmentStatus status);

  @Query(
      "SELECT COUNT(a) > 0 FROM Appointment a "
          + "WHERE a.patientId = :patientId AND a.doctorId = :doctorId "
          + "AND CAST(a.appointmentTime AS date) = :date "
          + "AND a.id <> :id AND a.status <> :status")
  boolean existsByPatientIdAndDoctorIdAndDateAndIdNotAndStatusNot(
      @Param("patientId") UUID patientId,
      @Param("doctorId") UUID doctorId,
      @Param("date") java.time.LocalDate date,
      @Param("id") UUID id,
      @Param("status") com.res.hms.clinical.domain.AppointmentStatus status);

  java.util.List<Appointment> findByPatientIdAndHospitalIdOrderByAppointmentTimeDesc(
      UUID patientId, UUID hospitalId);

  Page<Appointment> findByPatientIdAndHospitalIdOrderByAppointmentTimeDesc(
      UUID patientId, UUID hospitalId, Pageable pageable);

  @Query(
      "SELECT a FROM Appointment a "
          + "WHERE a.appointmentTime < :threshold "
          + "AND a.status NOT IN (com.res.hms.clinical.domain.AppointmentStatus.COMPLETED, com.res.hms.clinical.domain.AppointmentStatus.CANCELLED) "
          + "AND a.deleted = false")
  java.util.List<Appointment> findOverdueIncompleteAppointments(
      @Param("threshold") java.time.Instant threshold);
}
