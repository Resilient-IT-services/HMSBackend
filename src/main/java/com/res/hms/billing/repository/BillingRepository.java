package com.res.hms.billing.repository;

import com.res.hms.common.repository.BaseRepository;
import com.res.hms.domain.Billing;
import java.util.Optional;
import java.util.UUID;

public interface BillingRepository extends BaseRepository<Billing> {
  Optional<Billing> findByAppointmentIdAndHospitalId(UUID appointmentId, UUID hospitalId);
}
