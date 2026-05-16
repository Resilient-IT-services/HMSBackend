package com.res.hms.finance.application.service;

import com.res.hms.finance.domain.Payment;
import com.res.hms.finance.domain.PaymentMode;
import com.res.hms.finance.domain.PaymentStatus;
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

    @Transactional
    public Payment recordPayment(UUID hospitalId, UUID patientId, BigDecimal amount, PaymentMode mode, UUID collectedBy) {
        Payment payment = new Payment();
        payment.setHospitalId(hospitalId);
        payment.setPatientId(patientId);
        payment.setAmount(amount);
        payment.setPaymentMode(mode);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setCollectedBy(collectedBy);
        payment.setReceiptNumber("RCP-" + System.currentTimeMillis());
        return paymentRepository.save(payment);
    }
}
