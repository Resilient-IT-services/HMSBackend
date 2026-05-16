package com.res.hms.finance.infrastructure.repository;

import com.res.hms.finance.domain.PaymentAllocation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, UUID> {
    List<PaymentAllocation> findAllByInvoiceId(UUID invoiceId);
    List<PaymentAllocation> findAllByPaymentId(UUID paymentId);
}
