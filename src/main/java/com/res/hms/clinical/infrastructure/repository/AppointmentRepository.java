package com.res.hms.clinical.infrastructure.repository;

import com.res.hms.clinical.domain.Appointment;
import com.res.hms.core.repository.BaseRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository
    extends BaseRepository<Appointment>, JpaSpecificationExecutor<Appointment> {
  @Query(
      "SELECT COUNT(a) FROM Appointment a "
          + "WHERE a.hospitalId = :hospitalId AND "
          + "a.appointmentTime >= :threshold")
  long countUpcoming(
      @Param("hospitalId") UUID hospitalId, @Param("threshold") java.time.Instant threshold);

  @Query(
      "SELECT a FROM Appointment a "
          + "WHERE a.doctorId = :doctorId "
          + "AND a.appointmentTime >= :start AND a.appointmentTime < :end "
          + "AND a.status NOT IN (com.res.hms.clinical.domain.AppointmentStatus.CANCELLED)")
  java.util.List<Appointment> findByDoctorIdAndDateRange(
      @Param("doctorId") UUID doctorId,
      @Param("start") java.time.Instant start,
      @Param("end") java.time.Instant end);

  @Query(
      "SELECT MAX(a.tokenNumber) FROM Appointment a "
          + "WHERE a.doctorId = :doctorId "
          + "AND a.appointmentTime >= :start AND a.appointmentTime < :end")
  Integer findMaxTokenByDoctorAndDateRange(
      @Param("doctorId") UUID doctorId,
      @Param("start") java.time.Instant start,
      @Param("end") java.time.Instant end);

  @Query(
      "SELECT COUNT(a) > 0 FROM Appointment a "
          + "WHERE a.patientId = :patientId AND a.doctorId = :doctorId "
          + "AND a.appointmentTime >= :start AND a.appointmentTime < :end "
          + "AND a.status <> :status")
  boolean existsByPatientIdAndDoctorIdAndDateRangeAndStatusNot(
      @Param("patientId") UUID patientId,
      @Param("doctorId") UUID doctorId,
      @Param("start") java.time.Instant start,
      @Param("end") java.time.Instant end,
      @Param("status") com.res.hms.clinical.domain.AppointmentStatus status);

  @Query(
      "SELECT COUNT(a) > 0 FROM Appointment a "
          + "WHERE a.patientId = :patientId AND a.doctorId = :doctorId "
          + "AND a.appointmentTime >= :start AND a.appointmentTime < :end "
          + "AND a.id <> :id AND a.status <> :status")
  boolean existsByPatientIdAndDoctorIdAndDateRangeAndIdNotAndStatusNot(
      @Param("patientId") UUID patientId,
      @Param("doctorId") UUID doctorId,
      @Param("start") java.time.Instant start,
      @Param("end") java.time.Instant end,
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

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT a FROM Appointment a "
          + "WHERE a.hospitalId = :hospitalId "
          + "AND a.doctorId = :doctorId "
          + "AND a.appointmentTime >= :start AND a.appointmentTime < :end "
          + "AND a.deleted = false "
          + "AND a.status = com.res.hms.clinical.domain.AppointmentStatus.CONSULTATION_STARTED "
          + "ORDER BY a.tokenNumber ASC, a.appointmentTime ASC")
  java.util.List<Appointment> findRunningConsultationsForUpdateByDateRange(
      @Param("hospitalId") UUID hospitalId,
      @Param("doctorId") UUID doctorId,
      @Param("start") java.time.Instant start,
      @Param("end") java.time.Instant end);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT a FROM Appointment a "
          + "WHERE a.hospitalId = :hospitalId "
          + "AND a.doctorId = :doctorId "
          + "AND a.appointmentTime >= :start AND a.appointmentTime < :end "
          + "AND a.deleted = false "
          + "AND a.status IN ("
          + "com.res.hms.clinical.domain.AppointmentStatus.PAID, "
          + "com.res.hms.clinical.domain.AppointmentStatus.VITALS_RECORDED"
          + ") "
          + "ORDER BY a.tokenNumber ASC, a.appointmentTime ASC")
  java.util.List<Appointment> findWaitingAppointmentsForUpdateByDateRange(
      @Param("hospitalId") UUID hospitalId,
      @Param("doctorId") UUID doctorId,
      @Param("start") java.time.Instant start,
      @Param("end") java.time.Instant end,
      Pageable pageable);
}
