package com.res.hms.finance.application.dto;

import com.res.hms.finance.domain.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDTO {
    private UUID id;
    private UUID patientId;
    private String patientName;
    private UUID visitId;
    private String invoiceNumber;
    private InvoiceStatus status;
    private BigDecimal subtotalAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private List<InvoiceItemDTO> items;
}
