package com.res.hms.finance.application.dto;

import java.math.BigDecimal;

import com.res.hms.finance.domain.BillingStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBillingStatusRequest {
  @NotNull private BillingStatus status;
  private String paymentMethod;
  private BigDecimal discount;
}
