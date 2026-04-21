package com.res.hms.appointment.repository;

import com.res.hms.common.repository.BaseRepository;
import com.res.hms.domain.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends BaseRepository<Appointment, Long> {
  Page<Appointment> findAllByHospitalIdOrderByAppointmentTimeDesc(
      Long hospitalId, Pageable pageable);

  @Query(
      "SELECT a FROM Appointment a JOIN a.patient p JOIN a.doctor d JOIN d.user u "
          + "WHERE a.hospitalId = :hospitalId AND "
          + "(LOWER(p.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR "
          + "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) "
          + "ORDER BY a.appointmentTime DESC")
  Page<Appointment> search(
      @Param("hospitalId") Long hospitalId, @Param("query") String query, Pageable pageable);
}
