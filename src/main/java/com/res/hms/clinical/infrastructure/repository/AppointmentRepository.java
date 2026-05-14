package com.res.hms.clinical.infrastructure.repository;

import com.res.hms.clinical.domain.Appointment;
import com.res.hms.core.repository.BaseRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends BaseRepository<Appointment> {
  @Query("SELECT a FROM Appointment a LEFT JOIN FETCH a.patient LEFT JOIN FETCH a.doctor "
          + "WHERE a.hospitalId = :hospitalId "
          + "ORDER BY a.appointmentTime DESC")
  Page<Appointment> findAllByHospitalIdOrderByAppointmentTimeDesc(
      @Param("hospitalId") UUID hospitalId, Pageable pageable);

  @Query(
      "SELECT a FROM Appointment a LEFT JOIN FETCH a.patient LEFT JOIN FETCH a.doctor "
          + "WHERE a.hospitalId = :hospitalId AND "
          + "CAST(a.appointmentTime AS date) = :date "
          + "ORDER BY a.appointmentTime DESC")
  Page<Appointment> findAllByHospitalIdAndDate(
      @Param("hospitalId") UUID hospitalId,
      @Param("date") java.time.LocalDate date,
      Pageable pageable);

  @Query(
      "SELECT a FROM Appointment a LEFT JOIN FETCH a.patient LEFT JOIN FETCH a.doctor "
          + "WHERE a.hospitalId = :hospitalId AND "
          + "(LOWER(a.patientName) LIKE :query OR "
          + "LOWER(a.doctorName) LIKE :query) "
          + "ORDER BY a.appointmentTime DESC")
  Page<Appointment> search(
      @Param("hospitalId") UUID hospitalId, @Param("query") String query, Pageable pageable);

  @Query(
      "SELECT a.status, COUNT(a) FROM Appointment a "
          + "WHERE a.hospitalId = :hospitalId AND "
          + "CAST(a.appointmentTime AS date) = :date "
          + "GROUP BY a.status")
  java.util.List<Object[]> getStatsByDate(
      @Param("hospitalId") UUID hospitalId, @Param("date") java.time.LocalDate date);

  @Query(
      "SELECT COUNT(a) FROM Appointment a "
          + "WHERE a.hospitalId = :hospitalId AND "
          + "CAST(a.appointmentTime AS date) > :date")
  long countUpcoming(
      @Param("hospitalId") UUID hospitalId, @Param("date") java.time.LocalDate date);

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

    boolean existsByDoctorIdAndAppointmentTimeAndStatusNot(
        UUID doctorId,
        java.time.LocalDateTime appointmentTime,
        com.res.hms.clinical.domain.AppointmentStatus status
    );

  java.util.List<Appointment> findByPatientIdAndHospitalIdOrderByAppointmentTimeDesc(
      UUID patientId, UUID hospitalId);
}
