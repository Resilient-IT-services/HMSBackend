package com.res.hms.pharmacy.dto;

import com.res.hms.billing.dto.BillingResponse;

public record DispenseResponse(Long prescriptionId, BillingResponse billing) {}
