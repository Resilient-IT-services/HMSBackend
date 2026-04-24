package com.res.hms.pharmacy.dto;

import com.res.hms.billing.dto.BillingResponse;
import java.util.UUID;

public record DispenseResponse(UUID prescriptionId, BillingResponse billing) {}
