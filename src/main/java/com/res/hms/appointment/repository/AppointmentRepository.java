package com.res.hms.appointment.repository;

import com.res.hms.common.repository.BaseRepository;
import com.res.hms.domain.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppointmentRepository extends BaseRepository<Appointment, Long> {
  Page<Appointment> findAllByHospitalIdOrderByAppointmentTimeDesc(
      Long hospitalId, Pageable pageable);
}
