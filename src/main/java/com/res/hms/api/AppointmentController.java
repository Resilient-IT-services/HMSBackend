package com.res.hms.api;

import com.res.hms.appointment.dto.AppointmentResponse;
import com.res.hms.appointment.dto.CreateAppointmentRequest;
import com.res.hms.appointment.dto.UpdateAppointmentStatusRequest;
import com.res.hms.appointment.service.AppointmentService;
import com.res.hms.common.api.ApiResponse;
import com.res.hms.common.api.PageResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Appointments")
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

  private final AppointmentService appointmentService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
  public ApiResponse<AppointmentResponse> create(
      @Valid @RequestBody CreateAppointmentRequest request) {
    return ApiResponse.success(appointmentService.create(request));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','NURSE','DOCTOR')")
  public ApiResponse<PageResponse<AppointmentResponse>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search) {
    return ApiResponse.success(appointmentService.list(page, Math.min(size, 100), search));
  }

  @PostMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR')")
  public ApiResponse<AppointmentResponse> updateStatus(
      @PathVariable UUID id, @Valid @RequestBody UpdateAppointmentStatusRequest request) {
    return ApiResponse.success(appointmentService.updateStatus(id, request));
  }
}
