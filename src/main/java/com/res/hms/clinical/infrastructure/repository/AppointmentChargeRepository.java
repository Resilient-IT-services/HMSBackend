package com.res.hms.clinical.infrastructure.repository;

import com.res.hms.clinical.domain.AppointmentCharge;
import com.res.hms.core.repository.BaseRepository;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentChargeRepository extends BaseRepository<AppointmentCharge> {
  Optional<AppointmentCharge> findByAppointmentIdAndHospitalId(UUID appointmentId, UUID hospitalId);
}
