package com.res.hms.finance.application.dto;

import com.res.hms.finance.domain.PaymentMode;
import com.res.hms.finance.domain.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    private UUID id;
    private UUID patientId;
    private String receiptNumber;
    private BigDecimal amount;
    private PaymentMode paymentMode;
    private String referenceNumber;
    private PaymentStatus status;
    private java.time.Instant paymentDate;
    private UUID collectedBy;
}
