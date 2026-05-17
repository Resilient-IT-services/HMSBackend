package com.res.hms.finance.application.dto;

import com.res.hms.finance.domain.PaymentMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class RecordPaymentRequest {
    @NotNull private UUID patientId;
    
    private UUID visitId;
    
    @NotNull 
    @Min(0)
    private BigDecimal amount;
    
    @NotNull private PaymentMode paymentMode;
    
    private String referenceNumber;
    
    private UUID invoiceId; // Optional: directly allocate to this invoice
}
