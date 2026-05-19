package com.res.hms.finance.application.service;

import com.res.hms.finance.application.dto.PaymentDTO;
import com.res.hms.finance.application.dto.RecordPaymentRequest;
import com.res.hms.finance.domain.Payment;
import com.res.hms.finance.domain.PaymentAllocation;
import com.res.hms.finance.domain.PaymentMode;
import com.res.hms.finance.domain.PaymentStatus;
import com.res.hms.finance.infrastructure.repository.PaymentAllocationRepository;
import com.res.hms.finance.infrastructure.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final com.res.hms.finance.infrastructure.repository.InvoiceRepository invoiceRepository;

    @Transactional
    public PaymentDTO recordPayment(UUID hospitalId, RecordPaymentRequest request, UUID collectedBy) {
        Payment payment = new Payment();
        payment.setHospitalId(hospitalId);
        payment.setPatientId(request.getPatientId());
        payment.setVisitId(request.getVisitId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMode(request.getPaymentMode());
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(java.time.Instant.now());
        payment.setCollectedBy(collectedBy);
        payment.setReceiptNumber("RCP-" + System.currentTimeMillis());
        
        payment = paymentRepository.save(payment);

        if (request.getInvoiceId() != null) {
            allocatePayment(payment, request.getInvoiceId(), request.getAmount());
        }

        return toDto(payment);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void allocatePayment(Payment payment, UUID invoiceId, BigDecimal amount) {
        PaymentAllocation allocation = new PaymentAllocation();
        allocation.setPaymentId(payment.getId());
        allocation.setInvoiceId(invoiceId);
        allocation.setAllocatedAmount(amount);
        paymentAllocationRepository.save(allocation);
        
        // H2 Compatibility: Manually update invoice status since triggers don't run in H2
        invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
            BigDecimal totalPaid = invoice.getAmountPaid() != null ? invoice.getAmountPaid().add(amount) : amount;
            invoice.setAmountPaid(totalPaid);
            if (totalPaid.compareTo(invoice.getTotalAmount()) >= 0) {
                invoice.setStatus(com.res.hms.finance.domain.InvoiceStatus.PAID);
            } else {
                invoice.setStatus(com.res.hms.finance.domain.InvoiceStatus.PARTIAL);
            }
            invoiceRepository.save(invoice);
        });
    }

    public PaymentDTO toDto(Payment payment) {
        return PaymentDTO.builder()
                .id(payment.getId())
                .patientId(payment.getPatientId())
                .receiptNumber(payment.getReceiptNumber())
                .amount(payment.getAmount())
                .paymentMode(payment.getPaymentMode())
                .referenceNumber(payment.getReferenceNumber())
                .status(payment.getStatus())
                .paymentDate(payment.getPaymentDate())
                .collectedBy(payment.getCollectedBy())
                .build();
    }

    @Transactional
    public Payment recordPayment(UUID hospitalId, UUID patientId, BigDecimal amount, PaymentMode mode, UUID collectedBy) {
        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setPatientId(patientId);
        req.setAmount(amount);
        req.setPaymentMode(mode);
        return paymentRepository.findById(recordPayment(hospitalId, req, collectedBy).getId()).orElseThrow();
    }
}
