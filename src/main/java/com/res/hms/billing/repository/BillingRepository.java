package com.res.hms.billing.repository;

import com.res.hms.domain.Billing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingRepository extends JpaRepository<Billing, Long> {
    Optional<Billing> findByIdAndHospitalId(Long id, Long hospitalId);
}
