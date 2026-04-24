package com.res.hms.billing.dto;

import com.res.hms.domain.BillingSourceType;
import com.res.hms.domain.BillingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BillingResponse(
    UUID id,
    UUID patientId,
    BillingSourceType sourceType,
    BillingStatus status,
    BigDecimal totalAmount,
    List<BillingItemResponse> items,
    LocalDateTime createdAt) {}
