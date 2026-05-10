package com.res.hms.finance.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.res.hms.finance.domain.BillingSourceType;
import com.res.hms.finance.domain.BillingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingDTO {
  private UUID id;
  private UUID patientId;
  private String patientName;
  private UUID appointmentId;
  private BillingSourceType sourceType;
  private BillingStatus status;
  private BigDecimal totalAmount;
  private BigDecimal discount;
  private String paymentMethod;
  private LocalDateTime createdAt;
  private List<BillingItemDTO> items;
}
