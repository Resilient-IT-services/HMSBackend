package com.res.hms.finance.application.service;

import com.res.hms.finance.domain.Refund;
import com.res.hms.finance.domain.RefundStatus;
import com.res.hms.finance.infrastructure.repository.RefundRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundService {

  private final RefundRepository refundRepository;

  @Transactional
  public Refund initiateRefund(
      UUID hospitalId, UUID paymentId, UUID invoiceId, BigDecimal amount, String reason) {
    Refund refund = new Refund();
    refund.setHospitalId(hospitalId);
    refund.setPaymentId(paymentId);
    refund.setInvoiceId(invoiceId);
    refund.setRefundAmount(amount);
    refund.setReason(reason);
    refund.setStatus(RefundStatus.INITIATED);
    return refundRepository.save(refund);
  }

  @Transactional
  public void completeRefund(UUID refundId) {
    refundRepository
        .findById(refundId)
        .ifPresent(
            refund -> {
              refund.setStatus(RefundStatus.COMPLETED);
              refund.setProcessedAt(java.time.Instant.now());
              refundRepository.save(refund);
            });
  }
}
