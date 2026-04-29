package com.res.hms.api;

import com.res.hms.billing.dto.BillingDTO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

  @PutMapping("/{billingId}/status")
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','SUPER_ADMIN')")
  public ApiResponse<BillingDTO> updateStatus(
      @PathVariable UUID billingId, @RequestParam com.res.hms.domain.BillingStatus status) {
    return ApiResponse.success(billingService.updateBillingStatus(billingId, status));
  }
}
