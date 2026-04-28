package com.res.hms.appointment.repository;

import com.res.hms.common.repository.BaseRepository;
import com.res.hms.domain.Appointment;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends BaseRepository<Appointment> {
  Page<Appointment> findAllByHospitalIdOrderByAppointmentTimeDesc(
      UUID hospitalId, Pageable pageable);

  @Query(
      "SELECT a FROM Appointment a JOIN a.patient p JOIN a.doctorProfile d JOIN d.user u "
          + "WHERE a.hospitalId = :hospitalId AND "
          + "(LOWER(p.fullName) LIKE :query OR "
          + "LOWER(u.fullName) LIKE :query) "
          + "ORDER BY a.appointmentTime DESC")
  Page<Appointment> search(
      @Param("hospitalId") UUID hospitalId, @Param("query") String query, Pageable pageable);

  @Query(
      "SELECT a FROM Appointment a "
          + "WHERE a.doctorProfile.id = :doctorId "
          + "AND CAST(a.appointmentTime AS date) = :date "
          + "AND a.status NOT IN (com.res.hms.domain.AppointmentStatus.CANCELLED)")
  java.util.List<Appointment> findByDoctorIdAndDate(
      @Param("doctorId") UUID doctorId, @Param("date") java.time.LocalDate date);

  @Query(
      "SELECT MAX(a.tokenNumber) FROM Appointment a "
          + "WHERE a.doctorProfile.id = :doctorId "
          + "AND CAST(a.appointmentTime AS date) = :date")
  Integer findMaxTokenByDoctorAndDate(
      @Param("doctorId") UUID doctorId, @Param("date") java.time.LocalDate date);

  java.util.List<Appointment> findByPatientIdAndHospitalIdOrderByAppointmentTimeDesc(
      UUID patientId, UUID hospitalId);
}
