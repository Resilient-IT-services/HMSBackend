package com.res.hms.appointment.repository;

import com.res.hms.domain.Appointment;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
  Page<Appointment> findAllByHospitalIdOrderByAppointmentTimeDesc(
      Long hospitalId, Pageable pageable);

  Optional<Appointment> findByIdAndHospitalId(Long id, Long hospitalId);
}
