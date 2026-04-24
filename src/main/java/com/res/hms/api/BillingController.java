package com.res.hms.api;

import com.res.hms.billing.dto.AddBillingItemRequest;
import com.res.hms.billing.dto.BillingResponse;
import com.res.hms.billing.dto.CreateOpdBillingRequest;
import com.res.hms.billing.service.BillingService;
import com.res.hms.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Billing")
@RestController
@RequestMapping("/api/v1/billings")
@RequiredArgsConstructor
public class BillingController {

  private final BillingService billingService;

  @PostMapping("/opd")
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
  public ApiResponse<BillingResponse> createOpdBilling(
      @Valid @RequestBody CreateOpdBillingRequest request) {
    return ApiResponse.success(billingService.createOpdBilling(request));
  }

  @PostMapping("/{id}/items")
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','PHARMACIST')")
  public ApiResponse<BillingResponse> addItem(
      @PathVariable UUID id, @Valid @RequestBody AddBillingItemRequest request) {
    return ApiResponse.success(billingService.addItem(id, request));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','PHARMACIST')")
  public ApiResponse<BillingResponse> get(@PathVariable UUID id) {
    return ApiResponse.success(billingService.get(id));
  }
}
