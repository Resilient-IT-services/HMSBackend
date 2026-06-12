package com.res.hms.lab.application.dto;

import com.res.hms.finance.domain.PaymentMode;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class LabOrderFromRecommendationRequest {
  private UUID prescriptionId;
  private List<UUID> testIds;
  private PaymentMode paymentMode;
  private BigDecimal amountPaid;
  private String referenceNumber;
}
