package com.res.hms.billing.repository;

import com.res.hms.domain.Billing;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingRepository extends JpaRepository<Billing, Long> {
  Optional<Billing> findByIdAndHospitalId(Long id, Long hospitalId);
}
