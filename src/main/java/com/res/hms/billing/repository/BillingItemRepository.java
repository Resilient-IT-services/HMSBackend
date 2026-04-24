package com.res.hms.billing.repository;

import com.res.hms.domain.BillingItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingItemRepository extends JpaRepository<BillingItem, UUID> {}
