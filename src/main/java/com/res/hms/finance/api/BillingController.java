package com.res.hms.finance.api;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.finance.application.dto.BillingDTO;
import com.res.hms.finance.application.service.BillingService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Billing")
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

  private final BillingService billingService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','SUPER_ADMIN')")
  public ApiResponse<BillingDTO> create(@Valid @RequestBody BillingDTO request) {
    return ApiResponse.success(billingService.createBilling(request));
  }

  @GetMapping("/appointment/{appointmentId}")
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','SUPER_ADMIN')")
  public ApiResponse<BillingDTO> getByAppointment(@PathVariable UUID appointmentId) {
    return ApiResponse.success(billingService.getByAppointmentId(appointmentId));
  }

  @Deprecated(since = "v1-refactor", forRemoval = true)
  @PutMapping("/{billingId}/status")
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','SUPER_ADMIN')")
  public ApiResponse<BillingDTO> updateStatus(
      @PathVariable UUID billingId,
      @RequestParam com.res.hms.finance.domain.BillingStatus status,
      @RequestParam(required = false) String paymentMethod,
      @RequestParam(required = false) java.math.BigDecimal discount) {
    return ApiResponse.success(
        billingService.updateBillingStatus(billingId, status, paymentMethod, discount));
  }

  @PutMapping("/{billingId}/status-v2")
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','SUPER_ADMIN')")
  public ApiResponse<BillingDTO> updateStatusV2(
      @PathVariable UUID billingId,
      @Valid @RequestBody com.res.hms.finance.application.dto.UpdateBillingStatusRequest request) {
    return ApiResponse.success(
        billingService.updateBillingStatus(
            billingId, request.getStatus(), request.getPaymentMethod(), request.getDiscount()));
  }
}
